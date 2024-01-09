package org.zhaobo.ping.core;

import lombok.extern.slf4j.Slf4j;
import org.zhaobo.ping.common.constants.GatewayConst;
import org.zhaobo.ping.core.netty.NettyHttpClient;
import org.zhaobo.ping.core.netty.NettyHttpServer;
import org.zhaobo.ping.core.netty.process.DisruptorNettyCoreProcessor;
import org.zhaobo.ping.core.netty.process.NettyCoreProcessor;
import org.zhaobo.ping.core.netty.process.NettyProcessor;

/**
 * @Auther: bo
 * @Date: 2023/12/2 20:15
 * @Description:
 */
@Slf4j
public class Container implements LifeCyclye {
    private final Config config;

    private NettyHttpClient nettyHttpClient;

    private NettyHttpServer nettyHttpServer;

    private NettyProcessor nettyProcessor;

    public Container(Config config) {
        this.config = config;
        init();
    }


    @Override
    public void init() {
        NettyCoreProcessor nettyCoreProcessor = new NettyCoreProcessor();
        if (GatewayConst.BUFFER_TYPE_PARALLEL.equalsIgnoreCase(config.getBufferType())) {
            this.nettyProcessor = new DisruptorNettyCoreProcessor(config, nettyCoreProcessor);
        } else {
            this.nettyProcessor = nettyCoreProcessor;
        }
        this.nettyHttpServer = new NettyHttpServer(config, nettyProcessor);
        this.nettyHttpClient = new NettyHttpClient(config, nettyHttpServer.getEventLoopGroupWorker());
    }

    @Override
    public void start() {
        nettyProcessor.start();
        nettyHttpClient.start();
        nettyHttpServer.start();
        log.info("api-gateway started!");
    }

    @Override
    public void shutdown() {
        nettyProcessor.shutdown();
        nettyHttpServer.shutdown();
        nettyHttpClient.shutdown();
    }
}
