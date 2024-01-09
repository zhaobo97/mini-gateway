package org.zhaobo.ping.core.context;

import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @Auther: bo
 * @Date: 2023/11/30 16:13
 * @Description:
 */

public class BaseContext implements IContext{

    // 转发协议
    protected final String protocol;

    // 状态
    protected volatile int status = IContext.RUNNING;

    // Netty上下文
    protected final ChannelHandlerContext nettyCtx;

    // 上下文参数
    protected final Map<String, Object> attributes = new HashMap<>();

    // 异常
    protected Throwable throwable;

    // 是否保持长连接
    protected final boolean keepalive;

    // 回调函数集合
    protected List<Consumer<IContext>> callbacks;

    // 是否释放资源
    protected final AtomicBoolean requestReleased = new AtomicBoolean(false);

    public BaseContext(String protocol, ChannelHandlerContext nettyCtx, boolean keepalive) {
        this.protocol = protocol;
        this.nettyCtx = nettyCtx;
        this.keepalive = keepalive;
    }

    public Object getAttribute(String key){
        return attributes.get(key);
    }

    public void setAttributes(String key,Object value){
        attributes.put(key, value);
    }


    @Override
    public void setRunning() {
        status = IContext.RUNNING;
    }

    @Override
    public void setWritten() {
        status = IContext.WRITTEN;
    }

    @Override
    public void setCompleted() {
        status = IContext.COMPLETED;
    }

    @Override
    public void setTerminated() {
        status = IContext.TERMINATED;
    }

    @Override
    public boolean isRunning() {
        return status == IContext.RUNNING;
    }

    @Override
    public boolean isWritten() {
        return status == IContext.WRITTEN;
    }

    @Override
    public boolean isCompleted() {
        return status == IContext.COMPLETED;
    }

    @Override
    public boolean isTerminate() {
        return status == IContext.TERMINATED;
    }

    @Override
    public String getProtocol() {
        return this.protocol;
    }

    @Override
    public Object getRequest() {
        return null;
    }

    @Override
    public void setRequest(Object request) {

    }

    @Override
    public Object getResponse() {
        return null;
    }

    @Override
    public void setResponse(Object response) {

    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public void seThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public ChannelHandlerContext getNettyCtx() {
        return nettyCtx;
    }

    @Override
    public boolean isKeepalive() {
        return keepalive;
    }

    @Override
    public boolean releaseRequest() {
        return false;
    }

    @Override
    public void setCompletedCallback(Consumer<IContext> consumer) {
        if (callbacks == null) {
            callbacks = new ArrayList<>();
        }
        callbacks.add(consumer);
    }

    @Override
    public void invokeCompletedCallback() {
        if (callbacks != null){
            callbacks.forEach( call -> call.accept(this));
        }
    }
}
