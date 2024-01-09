package org.zhaobo.ping.core.netty;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.zhaobo.ping.core.Config;
import org.zhaobo.ping.core.LifeCyclye;
import org.zhaobo.ping.core.helper.AsyncHttpHelper;

import java.io.IOException;

/**
 * @Auther: bo
 * @Date: 2023/12/2 19:54
 * @Description:
 */
@Slf4j
public class NettyHttpClient implements LifeCyclye {

    private final Config config;

    private final EventLoopGroup eventLoopGroupWorker;

    private AsyncHttpClient asyncHttpClient;

    public NettyHttpClient(Config config, EventLoopGroup eventLoopGroupWorker) {
        this.config = config;
        this.eventLoopGroupWorker = eventLoopGroupWorker;
        init();
    }

    @Override
    public void init() {
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder()
                .setEventLoopGroup(eventLoopGroupWorker)
                .setConnectTimeout(config.getHttpConnectTimeout())
                .setRequestTimeout(config.getHttpRequestTimeout())
                .setMaxRedirects(config.getHttpMaxRequestRetry())
                .setAllocator(PooledByteBufAllocator.DEFAULT) // 池化的byteBuf分配器，提升性能
                .setMaxConnections(config.getHttpMaxConnections())
                .setMaxConnectionsPerHost(config.getHttpConnectionsPerHost())
                .setPooledConnectionIdleTimeout(config.getHttpPooledConnectionIdleTimeout());

        this.asyncHttpClient = new DefaultAsyncHttpClient(builder.build());

    }

    @Override
    public void start() {
        // 自己写的工具类，实际上就是给工具类内部的async-http-client 初始化，在路由过滤时调用单例
        AsyncHttpHelper.getInstance().initialized(asyncHttpClient);
    }

    @Override
    public void shutdown() {
        if (asyncHttpClient != null) {
            try {
                this. asyncHttpClient.close();
            } catch (IOException e) {
                log.error("NettyHttpClient shutdown error {}",e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}
