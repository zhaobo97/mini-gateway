package org.zhaobo.ping.core.disruptor;

/**
 * @Auther: bo
 * @Date: 2023/12/10 17:45
 * @Description: 多生产者多消费者处理接口
 */
public interface ParallelQueue<E> {

    void add(E event);

    void add(E... events);

    boolean tryAdd(E event);

    boolean tryAdd(E... event);

    void start();

    void shutdown();

    boolean isShutdown();
}
