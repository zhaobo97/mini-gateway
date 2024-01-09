package org.zhaobo.ping.core.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import org.zhaobo.ping.core.netty.process.NettyProcessor;

/**
 * @Auther: bo
 * @Date: 2023/12/2 16:33
 * @Description:
 */

public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {

    private final NettyProcessor nettyProcessor;

    public NettyHttpServerHandler(NettyProcessor nettyProcessor) {
        this.nettyProcessor = nettyProcessor;
    }


    @Override
    public void channelRead(ChannelHandlerContext channelContext, Object msg) throws Exception {

        FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;
        HttpRequestWrapper wrapper = new HttpRequestWrapper();
        wrapper.setFullHttpRequest(fullHttpRequest);
        wrapper.setCtx(channelContext);
        nettyProcessor.process(wrapper);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        System.out.println("----");
    }
}
