package org.zhaobo.core.netty.process;

import com.lmax.disruptor.dsl.ProducerType;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.zhaobo.core.disruptor.EventListener;
import org.zhaobo.core.disruptor.ParallelQueueHandler;
import org.zhaobo.core.helper.ResponseHelper;
import org.zhaobo.core.netty.HttpRequestWrapper;
import org.zhaobo.common.enums.ResponseCode;
import org.zhaobo.core.Config;

/**
 * @Auther: bo
 * @Date: 2023/12/11 13:14
 * @Description:
 */
@Slf4j
public class DisruptorNettyCoreProcessor implements NettyProcessor {

    private static final String THREAD_NAME_PREFIX = "gateway-queue-";
    private Config config;
    private NettyCoreProcessor nettyCoreProcessor;
    private ParallelQueueHandler<HttpRequestWrapper> parallelQueueHandler;



    public DisruptorNettyCoreProcessor(Config config, NettyCoreProcessor nettyCoreProcessor) {
        this.config = config;
        this.nettyCoreProcessor = nettyCoreProcessor;

        ParallelQueueHandler.Builder<HttpRequestWrapper> builder = new ParallelQueueHandler.Builder<HttpRequestWrapper>()
                .setBufferSize(config.getBufferSize())
                .setThreads(config.getProcessThread())
                .setProducerType(ProducerType.MULTI)
                .setNamePrefix(THREAD_NAME_PREFIX)
                .setWaitStrategy(config.getWaitStrategy())
                .setListener(new BatchEventListenerProcessor());

        this.parallelQueueHandler = builder.build();
    }

    public class BatchEventListenerProcessor implements EventListener<HttpRequestWrapper> {

        @Override
        public void onEvent(HttpRequestWrapper event) {
            nettyCoreProcessor.process(event);
        }

        @Override
        public void onException(Throwable throwable, long sequeue, HttpRequestWrapper event) {
            HttpRequest httpRequest = event.getFullHttpRequest();
            ChannelHandlerContext ctx = event.getCtx();
            try {
                FullHttpResponse fullHttpResponse = ResponseHelper.getProcessFailHttpResponse(ResponseCode.INTERNAL_ERROR);
                if (!HttpUtil.isKeepAlive(httpRequest)) {
                    ctx.writeAndFlush(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
                }else {
                    fullHttpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    ctx.writeAndFlush(fullHttpResponse);
                }
            } catch (Exception e) {
                log.error("BatchEventListenerProcessor on exception {}", e.getMessage());
                throw new RuntimeException("BatchEventListenerProcessor on exception {}",e);
            }
        }
    }

    @Override
    public void process(HttpRequestWrapper wrapper) {
        this.parallelQueueHandler.add(wrapper);
    }

    @Override
    public void start() {
        parallelQueueHandler.start();
    }

    @Override
    public void shutdown() {
        parallelQueueHandler.shutdown();
    }
}
