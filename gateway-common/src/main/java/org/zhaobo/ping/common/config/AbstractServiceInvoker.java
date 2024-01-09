package org.zhaobo.ping.common.config;

/**
 * @Auther: bo
 * @Date: 2023/12/4 11:33
 * @Description:
 */

public abstract class AbstractServiceInvoker implements ServiceInvoker{

    private String path;

    private Integer timeout;

    @Override
    public void setInvokerPath(String path) {
        this.path = path;
    }

    @Override
    public String getInvokePath() {
        return this.path;
    }

    @Override
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    @Override
    public Integer getTimeout() {
        return this.timeout;
    }
}
