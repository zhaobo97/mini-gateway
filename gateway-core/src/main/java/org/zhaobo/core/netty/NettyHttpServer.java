package org.zhaobo.core.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.zhaobo.core.netty.process.NettyProcessor;
import org.zhaobo.core.Config;
import org.zhaobo.core.LifeCyclye;
import org.zhaobo.common.utils.RemotingUtil;

import java.net.InetSocketAddress;

/**
 * @Auther: bo
 * @Date: 2023/12/2 15:44
 * @Description:
 */
@Slf4j
public class NettyHttpServer implements LifeCyclye {

    private final Config config;

    private ServerBootstrap serverBootstrap;

    private EventLoopGroup eventLoopGroupBoss;

    @Getter
    private EventLoopGroup eventLoopGroupWorker;

    private final NettyProcessor nettyProcessor;

    public NettyHttpServer(Config config, NettyProcessor nettyProcessor) {
        this.config = config;
        this.nettyProcessor = nettyProcessor;
        init();
    }

    @Override
    public void init() {
        if (useEpoll()){
            this.serverBootstrap = new ServerBootstrap();
            this.eventLoopGroupBoss = new EpollEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-boss-thread"));
            this.eventLoopGroupWorker = new EpollEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-worker-thread"));
        }else {
            this.serverBootstrap = new ServerBootstrap();
            this.eventLoopGroupBoss = new NioEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-boss-thread"));
            this.eventLoopGroupWorker = new NioEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-worker-thread"));
        }
    }

    public boolean useEpoll(){
        return RemotingUtil.isLinuxPlatform() && Epoll.isAvailable();
    }

    @Override
    public void start() {
        this.serverBootstrap
                .group(eventLoopGroupBoss, eventLoopGroupWorker)
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(config.getPort()))
                .option(ChannelOption.SO_BACKLOG, 1024)			//	sync + accept = backlog
                .option(ChannelOption.SO_REUSEADDR, true)   	//	tcp端口重绑定
                .option(ChannelOption.SO_KEEPALIVE, false)  	//  如果在两小时内没有数据通信的时候，TCP会自动发送一个活动探测数据报文
                .childOption(ChannelOption.TCP_NODELAY, true)   //	该参数的左右就是禁用Nagle算法，使用小数据传输时合并
                .childOption(ChannelOption.SO_SNDBUF, 65535)	//	设置发送数据缓冲区大小
                .childOption(ChannelOption.SO_RCVBUF, 65535)	//	设置接收数据缓冲区大小
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(
                                new HttpServerCodec(),                                  // http编解码
                                new HttpObjectAggregator(config.getMaxContentLength()), //请求报文聚合成FullHttpRequest
                                new HttpServerExpectContinueHandler(),
                                new NettyServerConnectManagerHandler(),
                                new NettyHttpServerHandler(nettyProcessor)
                        );
                    }
                });
        try {
            this.serverBootstrap.bind().sync();
            log.info("server startup on port {}",config.getPort());
        } catch (InterruptedException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        if (eventLoopGroupBoss != null){
            eventLoopGroupBoss.shutdownGracefully();
        }
        if (eventLoopGroupWorker != null){
            eventLoopGroupWorker.shutdownGracefully();
        }
    }
}
