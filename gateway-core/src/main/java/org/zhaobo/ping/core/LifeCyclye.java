package org.zhaobo.ping.core;

/**
 * @Auther: bo
 * @Date: 2023/12/2 12:31
 * @Description:
 */
public interface LifeCyclye {

    // 初始化
    void init();

    // 启动
    void start();

    // 优雅关机
    void shutdown();
}
