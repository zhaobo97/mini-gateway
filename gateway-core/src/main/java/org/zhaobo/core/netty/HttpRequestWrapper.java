package org.zhaobo.core.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Data;

/**
 * @Auther: bo
 * @Date: 2023/12/2 16:47
 * @Description:
 */

@Data
public class HttpRequestWrapper {
    private FullHttpRequest fullHttpRequest;
    private ChannelHandlerContext ctx;
}
