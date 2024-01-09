package org.zhaobo.ping.core.filter.route;

import com.netflix.hystrix.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhaobo.ping.common.config.Rule;
import org.zhaobo.ping.common.constants.FilterConst;
import org.zhaobo.ping.common.enums.ResponseCode;
import org.zhaobo.ping.common.exception.ConnectException;
import org.zhaobo.ping.common.exception.ResponseException;
import org.zhaobo.ping.core.ConfigLoader;
import org.zhaobo.ping.core.context.GatewayContext;
import org.zhaobo.ping.core.filter.Filter;
import org.zhaobo.ping.core.filter.FilterInfo;
import org.zhaobo.ping.core.helper.AsyncHttpHelper;
import org.zhaobo.ping.core.helper.ResponseHelper;
import org.zhaobo.ping.core.response.GatewayResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * @Auther: bo
 * @Date: 2023/12/6 19:20
 * @Description: 路由过滤器，实现失败重试
 */

@Slf4j
@FilterInfo(id = FilterConst.ROUTER_FILTER_ID,
        name = FilterConst.ROUTER_FILTER_NAME,
        order = FilterConst.ROUTER_FILTER_ORDER)
public class RouterFilter implements Filter {
    private static final Logger accessLog = LoggerFactory.getLogger("accessLog");
    private boolean flag;

    @Override
    public void doFilter(GatewayContext ctx) {
        Rule.HystrixConfig hystrixConfig = getHystrixConfig(ctx);
        if (hystrixConfig != null) {
            routeWhithHystrix(ctx, hystrixConfig);
        } else {
            route(ctx, hystrixConfig);
        }
    }

    private void routeWhithHystrix(GatewayContext ctx, Rule.HystrixConfig hystrixConfig) {
        HystrixCommand.Setter setter = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory
                        .asKey(ctx.getUniqueId()))
                .andCommandKey(HystrixCommandKey.Factory
                        .asKey(ctx.getRequest().getPath()))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        .withCoreSize(hystrixConfig.getCoreThreadSize()))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                        .withExecutionIsolationThreadInterruptOnTimeout(true)
                        .withExecutionTimeoutInMilliseconds(hystrixConfig.getTimeout())
                        .withExecutionTimeoutEnabled(hystrixConfig.isTimeoutEnabled())
                        .withMetricsRollingStatisticalWindowInMilliseconds(hystrixConfig.getSlidingWindowDuration()) //统计的滑动窗口范围
                        .withMetricsRollingStatisticalWindowBuckets(hystrixConfig.getNumberOfWindowSegments()) //滑动窗口被分割的段数
                        .withCircuitBreakerRequestVolumeThreshold(hystrixConfig.getRequestTimesThreshold()) //请求次数
                        .withCircuitBreakerErrorThresholdPercentage(hystrixConfig.getFailureRateThreshold()) //失败率
                        .withCircuitBreakerSleepWindowInMilliseconds(hystrixConfig.getCircuitBreakerResetTime()) //熔断时间
                        .withCircuitBreakerEnabled(hystrixConfig.isCircuitBreakerEnabled()) // 启用熔断器
                );

        new HystrixCommand<>(setter) {
            @Override
            protected Object run() throws Exception {
                route(ctx, hystrixConfig);
                return null;
            }

            @Override
            protected Object getFallback() {
                flag = true;
                ctx.setWritten();
                ctx.releaseRequest();
                String fallbackMessage = hystrixConfig.getFallbackMessage();
                ByteBuf byteBuf = Unpooled.copiedBuffer(fallbackMessage, CharsetUtil.UTF_8);
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                        ResponseCode.SUCCESS.getHttpResponseStatus(),
                        byteBuf);
                ctx.getNettyCtx().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                ctx.setCompleted();
                log.info("************* 熔断 : {} ******************", ResponseCode.REQUEST_TIMEOUT.getMessage());
                return null;
            }
        }.execute();
    }

    private Rule.HystrixConfig getHystrixConfig(GatewayContext ctx) {
        Set<Rule.HystrixConfig> hystrixConfigs = ctx.getRule().getHystrixConfigs();
        Optional<Rule.HystrixConfig> optional = hystrixConfigs.stream().filter(c ->
                ctx.getRequest().getPath().equalsIgnoreCase(c.getPath())).findFirst();
        return optional.orElse(null);
    }

    /**
     * 路由到单异步处理或双异步处理
     *
     * @param gatewayContext
     */
    private void route(GatewayContext gatewayContext, Rule.HystrixConfig config) {
        try {
            Request request = gatewayContext.getRequest().build();
            // 执行请求
            CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);
            // 单异步
            boolean whenComplete = ConfigLoader.getInstance().getConfig().isWhenComplete();
            if (whenComplete) {
                future.whenComplete((futureResponse, throwable) -> {
                    complete(request, futureResponse, gatewayContext, throwable, config);
                });
            } else {
                future.whenCompleteAsync((futureResponse, throwable) -> {
                    complete(request, futureResponse, gatewayContext, throwable, config);
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 1.释放资源
     * 2.设置响应或者异常
     * 3.写回数据
     *
     * @param request
     * @param futureResponse
     * @param gatewayContext
     * @param throwable
     */
    private void complete(Request request,
                          Response futureResponse,
                          GatewayContext gatewayContext,
                          Throwable throwable,
                          Rule.HystrixConfig config) {
        //如果已经触发了getFallback了，就不再执行这段逻辑了，避免出现问题
        if (flag){
            flag = false;
            return;
        }

        // 失败重试
        //获取已重试的次数
        int currentRetryTimes = gatewayContext.getCurrentRetryTimes();
        //获取配置里面设置的需要重试的次数
        int confRetryTimes = gatewayContext.getRule().getRetryConfig().getTimes();
        // 如果是IO异常或者超时异常就可以重试
        if (throwable instanceof IOException || throwable instanceof TimeoutException
                && (currentRetryTimes < confRetryTimes) && Objects.nonNull(config)) {
            // 递归，
            doRetry(gatewayContext, currentRetryTimes);
            return;
        }

        // 失败重试结束，返回响应
        try {
            // 释放请求资源
            gatewayContext.releaseRequest();
            // 路由，设置响应或者异常
            if (Objects.nonNull(throwable)) {
                String url = request.getUrl();
                if (throwable instanceof TimeoutException) {
                    // 超时异常
                    gatewayContext.setThrowable(new ResponseException(ResponseCode.REQUEST_TIMEOUT));
                    gatewayContext.setResponse(GatewayResponse.buildOnFailure(ResponseCode.REQUEST_TIMEOUT));
                    log.info("************* REQUEST_TIMEOUT : {} ******************", ResponseCode.REQUEST_TIMEOUT.getMessage());
                    log.warn("complete timeout {}", url);
                } else {
                    // 其他连接异常
                    String uniqueId = gatewayContext.getUniqueId();
                    gatewayContext.setResponse(GatewayResponse.buildOnFailure(ResponseCode.HTTP_RESPONSE_ERROR));
                    gatewayContext.setThrowable(new ConnectException(throwable,
                            uniqueId, url, ResponseCode.HTTP_RESPONSE_ERROR));
                }
            } else {
                // 正常响应
                gatewayContext.setResponse(GatewayResponse.build(futureResponse));
            }
        } catch (Exception e) {
            gatewayContext.setThrowable(new ResponseException(ResponseCode.INTERNAL_ERROR));
            gatewayContext.setResponse(GatewayResponse.buildOnFailure(ResponseCode.INTERNAL_ERROR));
            log.error("complete error", e);
            throw new ResponseException(ResponseCode.INTERNAL_ERROR);
        } finally {
            // 写回响应数据
            gatewayContext.setWritten();
            ResponseHelper.writeResponse(gatewayContext);

            accessLog.info("------------------- 响应成功：耗时：{} {} {} {} {} {} , response body :{} -----------------",
                    System.currentTimeMillis() - gatewayContext.getRequest().getBeginTime(),
                    gatewayContext.getRequest().getClientIP(),
                    gatewayContext.getRequest().getUniqueId(),
                    gatewayContext.getRequest().getHttpMethod(),
                    gatewayContext.getRequest().getPath(),
                    gatewayContext.getResponse().getHttpResponseStatus().code(),
                    gatewayContext.getResponse().getFutureResponse().getResponseBody());
        }
    }

    /**
     * 重试方法在route filter处实现，重试就只需要在执行一遍filter就行
     *
     * @param gatewayContext
     * @param currentTimes
     */
    private void doRetry(GatewayContext gatewayContext, int currentTimes) {
        log.info("=============== 第{}次重试===============", currentTimes+1);
        gatewayContext.setCurrentRetryTimes(currentTimes + 1);
        doFilter(gatewayContext);
    }

    @Override
    public int getOrder() {
        return Filter.super.getOrder();
    }
}
