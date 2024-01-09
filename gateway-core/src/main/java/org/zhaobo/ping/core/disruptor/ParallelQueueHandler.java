package org.zhaobo.ping.core.disruptor;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.Setter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @Auther: bo
 * @Date: 2023/12/10 17:48
 * @Description: 基于Disruptor实现的多生产者多消费者无锁队列
 */

public class ParallelQueueHandler<E> implements ParallelQueue<E> {

    private RingBuffer<Holder> ringBuffer;

    private EventListener<E> listener;

    private WorkerPool<Holder> workerPool;

    private ExecutorService executorService;

    private EventTranslatorOneArg<Holder, E> eventTranslator;

    public ParallelQueueHandler(Builder<E> builder) {
        this.listener = builder.listener;
        this.executorService = Executors.newFixedThreadPool(builder.threads,
                new ThreadFactoryBuilder().setNameFormat("ParallelQueueHandler" +
                        builder.namePrefix + "-pool-%d").build());
        this.eventTranslator = new HolderEventTranslator();

        // 创建RingBuffer
        RingBuffer<Holder> buffer = RingBuffer.create(builder.producerType,
                new HolderEventFactory(), builder.bufferSize, builder.waitStrategy);

        // 通过RingBuffer创建屏障
        SequenceBarrier sequenceBarrier = buffer.newBarrier();

        // 创建多个消费者组
        WorkHandler<Holder>[] workHandlers = new WorkHandler[builder.threads];
        for (WorkHandler<Holder> workHandler : workHandlers) {
            workHandler = new HolderWorkerHandler();
        }

        // 创建多个消费者线程池
        WorkerPool<Holder> pool = new WorkerPool<>(buffer, sequenceBarrier, new HolderExceptionHandler(), workHandlers);

        // 设置多消费者的Sequence序号，用于统计消费进度
        buffer.addGatingSequences(pool.getWorkerSequences());
        this.workerPool = pool;
        this.ringBuffer = buffer;
    }

    @Override
    public void add(E event) {
        final RingBuffer<Holder> ringBuffer1 = this.ringBuffer;
        if (ringBuffer1 == null) {
            process(listener, new IllegalStateException("ParallelQueueHandler is closed"), event);
        }
        try {
            ringBuffer1.publishEvent(this.eventTranslator, event);
        } catch (NullPointerException e) {
            process(listener, new IllegalStateException("ParallelQueueHandler is closed"), event);
        }
    }

    @Override
    public void add(E... events) {
        final RingBuffer<Holder> ringBuffer1 = this.ringBuffer;
        if (ringBuffer1 == null) {
            process(listener, new IllegalStateException("ParallelQueueHandler is closed"), events);
        }
        try {
            ringBuffer1.publishEvents(this.eventTranslator, events);
        } catch (NullPointerException e) {
            process(listener, new IllegalStateException("ParallelQueueHandler is closed"), events);
        }
    }

    @Override
    public boolean tryAdd(E event) {
        final RingBuffer<Holder> ringBuffer1 = this.ringBuffer;
        if (ringBuffer1 == null){
            return false;
        }
        try {
            return ringBuffer1.tryPublishEvent(this.eventTranslator, event);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean tryAdd(E... events) {
        final RingBuffer<Holder> ringBuffer1 = this.ringBuffer;
        if (ringBuffer1 == null){
            return false;
        }
        try {
            return ringBuffer1.tryPublishEvents(this.eventTranslator, events);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void start() {
        this.ringBuffer = workerPool.start(executorService);
    }

    @Override
    public void shutdown() {
        if (ringBuffer != null) {
            ringBuffer = null;
        }
        if (workerPool != null) {
            workerPool.drainAndHalt();
        }
        if (executorService != null){
            executorService.shutdown();
        }
    }

    @Override
    public boolean isShutdown() {
        return ringBuffer == null;
    }

    private static <E> void process(EventListener<E> listener, Throwable throwable, E event) {
        listener.onException(throwable, -1, event);
    }

    private static <E> void process(EventListener<E> listener, Throwable throwable, E... events) {
        for (E event : events) {
            process(listener, throwable, event);
        }
    }

    private class Holder {
        @Setter
        private E event;
    }

    public static class Builder<E> {
        private ProducerType producerType = ProducerType.MULTI;
        private int bufferSize = 1024 * 16;
        private int threads = 1;
        private String namePrefix = "";
        private WaitStrategy waitStrategy = new BlockingWaitStrategy();
        private EventListener<E> listener;

        public Builder<E> setProducerType(ProducerType producerType) {
            Preconditions.checkNotNull(producerType);
            this.producerType = producerType;
            return this;
        }

        public Builder<E> setBufferSize(int bufferSize) {
            Preconditions.checkArgument(Integer.bitCount(bufferSize) == 1);
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder<E> setThreads(int threads) {
            Preconditions.checkArgument(threads > 0);
            this.threads = threads;
            return this;
        }

        public Builder<E> setNamePrefix(String namePrefix) {
            Preconditions.checkNotNull(namePrefix);
            this.namePrefix = namePrefix;
            return this;
        }

        public Builder<E> setWaitStrategy(WaitStrategy waitStrategy) {
            Preconditions.checkNotNull(waitStrategy);
            this.waitStrategy = waitStrategy;
            return this;
        }

        public Builder<E> setListener(EventListener<E> listener) {
            Preconditions.checkNotNull(listener);
            this.listener = listener;
            return this;
        }

        public ParallelQueueHandler<E> build() {
            return new ParallelQueueHandler<>(this);
        }
    }

    private class HolderWorkerHandler implements WorkHandler<Holder> {
        @Override
        public void onEvent(Holder holder) throws Exception {
            listener.onEvent(holder.event);
            holder.setEvent(null);
        }
    }

    private class HolderEventTranslator implements EventTranslatorOneArg<Holder, E> {
        @Override
        public void translateTo(Holder holder, long l, E e) {
            holder.setEvent(e);
        }
    }

    private class HolderEventFactory implements EventFactory<Holder> {
        @Override
        public Holder newInstance() {
            return new Holder();
        }
    }

    private class HolderExceptionHandler implements ExceptionHandler<Holder> {
        @Override
        public void handleEventException(Throwable throwable, long l, Holder holder) {
            try {
                listener.onException(throwable, l, holder.event);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                holder.setEvent(null);
            }
        }

        @Override
        public void handleOnStartException(Throwable throwable) {
            throw new UnsupportedOperationException(throwable);
        }

        @Override
        public void handleOnShutdownException(Throwable throwable) {
            throw new UnsupportedOperationException(throwable);
        }
    }
}
