package org.zhaobo.ping.core.disruptor;

/**
 * @Auther: bo
 * @Date: 2023/12/10 17:52
 * @Description:
 */
public interface EventListener<E> {

    void onEvent(E event);

    /**
     *
     * @param throwable 异常
     * @param sequeue 执行顺序
     * @param event 事件
     */
    void onException(Throwable throwable, long sequeue, E event);
}
