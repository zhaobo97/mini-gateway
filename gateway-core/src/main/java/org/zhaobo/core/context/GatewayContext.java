package org.zhaobo.core.context;

import io.micrometer.core.instrument.Timer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;
import org.zhaobo.core.request.GatewayRequest;
import org.zhaobo.core.response.GatewayResponse;
import org.zhaobo.common.config.Rule;
import org.zhaobo.common.utils.AssertUtil;

/**
 * @Auther: bo
 * @Date: 2023/11/30 16:57
 * @Description: 网关上下文
 */
@Getter
@Setter
public class GatewayContext extends BaseContext {

    private GatewayRequest request;

    private GatewayResponse response;

    private Rule rule;

    private boolean gray;

    private Throwable throwable;

    private int currentRetryTimes;

    private Timer.Sample timerSample;

    public GatewayContext(String protocol, ChannelHandlerContext nettyCtx, boolean keepalive, GatewayRequest request, Rule rule, int currentTimes) {
        super(protocol, nettyCtx, keepalive);
        this.request = request;
        this.rule = rule;
        this.currentRetryTimes = currentTimes;
    }

    public static Builder builder(){
        return new Builder();
    }


    public static class Builder {
        private ChannelHandlerContext nettyCtx;
        private String protocol;
        private Rule rule;
        private boolean keepalive;
        private GatewayRequest request;

        public Builder nettyCtx(ChannelHandlerContext nettyCtx) {
            this.nettyCtx = nettyCtx;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder rule(Rule rule) {
            this.rule = rule;
            return this;
        }

        public Builder keepalive(boolean keepalive) {
            this.keepalive = keepalive;
            return this;
        }

        public Builder request(GatewayRequest request) {
            this.request = request;
            return this;
        }

        public GatewayContext build() {
            AssertUtil.notNull(protocol, "protocol 不能为空");
            AssertUtil.notNull(nettyCtx, "nettyCtx 不能为空");
            AssertUtil.notNull(rule, "rule 不能为空");
            AssertUtil.notNull(request, "request 不能为空");
            return new GatewayContext(protocol, nettyCtx, keepalive, request, rule, 0);
        }
    }

    /**
     * 获取必要的上下文参数
     * @param key
     * @return
     */
    public Object getRequiredAttribute(String key) {
        Object value = getAttribute(key);
        AssertUtil.notNull(value, "缺乏必要参数");
        return value;
    }

    public Object getRequiredAttribute(String key, Object defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }

    /**
     * 获取指定的过滤器信息
     * @param id
     * @return
     */
    public Rule.FilterConfig getFilterConfig(String id){
        return rule.getFilterConfig(id);
    }

    /**
     * 获取服务id
     * @return
     */
    public String getUniqueId(){
        return request.getUniqueId();
    }


    /**
     * 重写父类 释放资源
     * @return boolean
     */
    public boolean releaseRequest(){
        if (requestReleased.compareAndSet(false, true)){
            ReferenceCountUtil.release(request.getFullHttpRequest());
        }
        return true;
    }

    public GatewayRequest getOriginRequest(){
        return request;
    }
}
