package org.zhaobo.ping.core.context;

import io.netty.channel.ChannelHandlerContext;

import java.util.function.Consumer;

/**
 * @Auther: bo
 * @Date: 2023/11/29 17:24
 * @Description:
 */
public interface IContext {

    // 上下文生命周期，运行中状态
    int RUNNING = -1;

    // 运行过程中发生错误，对其进行标记，告诉我们请求已经结束，需要返回客户端
    int WRITTEN = 0;

    // 标记写回成功，防止并发情况多次写回
    int COMPLETED = 1;

    // 表示网关请求结束
    int TERMINATED = 2;

    // 设置上下文状态为运行中
    void setRunning();

    // 设置上下文状态为标记写回
    void setWritten();

    // 设置上下文状态为写回成功
    void setCompleted();

    // 设置上下文状态为请求结束
    void setTerminated();

    // 判断网关状态
    boolean isRunning();
    boolean isWritten();
    boolean isCompleted();
    boolean isTerminate();

    // 获取协议
    String getProtocol();

    // 获取请求
    Object getRequest();

    void setRequest(Object request);

    // 获取响应
    Object getResponse();
    void setResponse(Object response);

    // 获取异常
    Throwable getThrowable();

    void seThrowable(Throwable throwable);

    // 获取netty上下文
    ChannelHandlerContext getNettyCtx();

    // 是否连接
    boolean isKeepalive();

    // 释放请求资源
    boolean releaseRequest();

    // 设置写回接收的回调函数
    void setCompletedCallback(Consumer<IContext> consumer);

    void invokeCompletedCallback();

}
