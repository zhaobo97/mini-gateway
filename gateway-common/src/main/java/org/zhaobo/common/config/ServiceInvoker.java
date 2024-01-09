package org.zhaobo.common.config;

/**
 * @Auther: bo
 * @Date: 2023/12/4 11:30
 * @Description:
 */
public interface ServiceInvoker {

    void setInvokerPath(String path);

    String getInvokePath();

    void setTimeout(Integer timeout);

    Integer getTimeout();
}
