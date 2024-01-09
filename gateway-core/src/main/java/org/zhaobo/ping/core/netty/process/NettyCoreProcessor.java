package org.zhaobo.ping.core.netty.process;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.zhaobo.ping.common.enums.ResponseCode;
import org.zhaobo.ping.common.exception.BaseException;
import org.zhaobo.ping.core.context.GatewayContext;
import org.zhaobo.ping.core.filter.FilterChainFactory;
import org.zhaobo.ping.core.filter.GatewayFilterChainFactory;
import org.zhaobo.ping.core.helper.RequestHelper;
import org.zhaobo.ping.core.helper.ResponseHelper;
import org.zhaobo.ping.core.netty.HttpRequestWrapper;

/**
 * @Auther: bo
 * @Date: 2023/12/2 17:37
 * @Description:
 */
@Slf4j
public class NettyCoreProcessor implements NettyProcessor {

    private final FilterChainFactory filterChainFactory = GatewayFilterChainFactory.getInstance();

    @Override
    public void process(HttpRequestWrapper wrapper) {
        ChannelHandlerContext channelContext = wrapper.getCtx();
        FullHttpRequest fullHttpRequest = wrapper.getFullHttpRequest();

        try {
            // 构建上下文
            GatewayContext gatewayContext = RequestHelper.doContext(fullHttpRequest, channelContext);
            // 对上下文执行过滤
            filterChainFactory.buildFilterChain(gatewayContext).doFilter(gatewayContext);

        } catch (BaseException e) {
            log.error("process error {} {}", e.getCode().getAppCode(), e.getCode().getMessage());
            // 异常时兜底
            FullHttpResponse fullHttpResponse = ResponseHelper.getProcessFailHttpResponse(e.getCode());
            // 这个地方的写回，是为了防止在route filter的写回之前出现异常
            doWriteAndRelease(channelContext, fullHttpRequest, fullHttpResponse);
        } catch (Throwable t) {
            log.error("process unknow error {}", t.getMessage());
            t.printStackTrace();
            FullHttpResponse fullHttpResponse = ResponseHelper.getProcessFailHttpResponse(ResponseCode.INTERNAL_ERROR);
            // 兜底的兜底
            doWriteAndRelease(channelContext, fullHttpRequest, fullHttpResponse);
        }

    }

    @Override
    public void start() {
        // 串行的netty处理什么都不需要做
    }

    @Override
    public void shutdown() {
        // 串行的netty处理什么都不需要做
    }

    /**
     * 1.回写数据
     * 2.释放资源
     *
     * @param ctx
     * @param fullHttpRequest
     * @param fullHttpResponse
     */
    private void doWriteAndRelease(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, FullHttpResponse fullHttpResponse) {
        ctx.writeAndFlush(fullHttpResponse)
                .addListener(ChannelFutureListener.CLOSE); // 释放资源后关闭channel
        ReferenceCountUtil.release(fullHttpRequest); // 释放request中的一些byteBuffer

    }

}
