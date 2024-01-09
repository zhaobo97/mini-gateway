package org.zhaobo.core.netty.process;

import org.zhaobo.core.netty.HttpRequestWrapper;

/**
 * @Auther: bo
 * @Date: 2023/12/2 16:50
 * @Description:
 */
public interface NettyProcessor {
    void process(HttpRequestWrapper wrapper);

    void start();

    void shutdown();
}
