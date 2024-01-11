package org.zhaobo.core.filter.route;

import com.netflix.hystrix.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.TraceCrossThread;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhaobo.core.context.GatewayContext;
import org.zhaobo.core.filter.FilterInfo;
import org.zhaobo.core.helper.AsyncHttpHelper;
import org.zhaobo.core.helper.ResponseHelper;
import org.zhaobo.core.response.GatewayResponse;
import org.zhaobo.common.config.Rule;
import org.zhaobo.common.constants.FilterConst;
import org.zhaobo.common.enums.ResponseCode;
import org.zhaobo.common.exception.ConnectException;
import org.zhaobo.common.exception.ResponseException;
import org.zhaobo.core.ConfigLoader;
import org.zhaobo.core.filter.Filter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

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

    /**
     * 1. 当一个服务调用被包装在HystrixCommand中时，Hystrix首先检查熔断器的状态。如果熔断器打开，则直接执行回退逻辑，不再执行实际的服务调用。
     * 2. 如果熔断器是关闭的，Hystrix执行服务调用。如果调用成功，Hystrix返回结果；如果调用失败或超时，Hystrix根据配置的降级逻辑执行回退操作。
     * 3. 在执行命令时，Hystrix还会监视命令的性能指标，如响应时间、错误率等。如果这些性能指标超过了预定的阈值，熔断器可以打开以防止继续请求失败。
     * 具体的，在滑动窗口范围内，请求次数超过设置的阈值、或者失败率达到设置的阈值、或者请求超时，就会打开熔断器一段时间。
     * 4. 如果熔断器在一段时间内保持打开状态，那么在一段时间后会尝试半开状态，允许一些请求通过以测试服务是否恢复正常。
     *
     * @param ctx
     * @param hystrixConfig
     */
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
//                flag = true;
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
     * whenComplete是一个非异步的完成方法。
     * 当CompletableFuture的执行完成或者发生异常时，它提供了一个回调。
     * 这个回调将在CompletableFuture执行的相同线程中执行。这意味着，如果CompletableFuture的操作是阻塞的，那么回调也会在同一个阻塞的线程中执行。
     * 在这段代码中，如果whenComplete为true，则在future完成时使用whenComplete方法。这意味着complete方法将在future所在的线程中被调用。
     * <p>
     * whenCompleteAsync是异步的完成方法。
     * 它也提供了一个在CompletableFuture执行完成或者发生异常时执行的回调。
     * 与whenComplete不同，这个回调将在不同的线程中异步执行。通常情况下，它将在默认的ForkJoinPool中的某个线程上执行，除非提供了自定义的Executor。
     * 在代码中，如果whenComplete为false，则使用whenCompleteAsync。这意味着complete方法将在不同的线程中异步执行。
     *
     * @param gatewayContext
     */
    private void route(GatewayContext gatewayContext, Rule.HystrixConfig config) {
        log.info("route");
        try {
            Request request = gatewayContext.getRequest().build();
            // 执行请求
            CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);
            boolean whenComplete = ConfigLoader.getInstance().getConfig().isWhenComplete();
            if (whenComplete) {
                future.whenComplete(new TraceBiConsumer(request, gatewayContext, config));
            } else {
                future.whenCompleteAsync(new TraceBiConsumer(request, gatewayContext, config));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @TraceCrossThread
    public class TraceBiConsumer implements BiConsumer<Response, Throwable> {
        private final Request request;
        private final GatewayContext gatewayContext;
        private final Rule.HystrixConfig config;
        private Response response;
        private Throwable throwable;

        public TraceBiConsumer(Request request, GatewayContext gatewayContext, Rule.HystrixConfig config) {
            this.request = request;
            this.gatewayContext = gatewayContext;
            this.config = config;
        }

        @Override
        public void accept(Response response, Throwable throwable) {
            this.response = response;
            this.throwable = throwable;
            run();
        }

        /**
         * skywalking会通过 @TraceCrossThread 注解增强run方法
         */
        private void run() {
            complete(request, response, gatewayContext, throwable, config);
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
//        //如果已经触发了getFallback了，就不再执行这段逻辑了，避免出现问题
//        if (flag){
//            flag = false;
//            return;
//        }

        log.info("complete");

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
        log.info("=============== 第{}次重试===============", currentTimes + 1);
        gatewayContext.setCurrentRetryTimes(currentTimes + 1);
        doFilter(gatewayContext);
    }

    @Override
    public int getOrder() {
        return Filter.super.getOrder();
    }
}
