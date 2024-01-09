package org.zhaobo.core;

import com.lmax.disruptor.*;
import lombok.Getter;
import lombok.Setter;

/**
 * @Auther: bo
 * @Date: 2023/12/2 10:43
 * @Description:
 */

@Setter
@Getter
public class Config {

    // 启动端口
    private int port = 8888;
    // 普罗米修斯数据采集端口
    private int prometheusPort = 18000;
    // 应用名称
    private String applicationName = "api-gateway";
    // 注册中心
    private String registryAddress = "192.168.25.131:8848";

    // 注册中心
    //空格代表的就是public的命名空间id
    private String namespace = "";
    private String group = "DEFAULT_GROUP";
    private String username = "nacos";
    private String password = "nacos";

    // netty
    // boss线程的线程数
    private int eventLoopGroupBossNum = 1;

    // worker线程数
    private int eventLoopGroupWorkerNum = Runtime.getRuntime().availableProcessors();

    // http报文大小限制
    private int maxContentLength = 64 * 1024 *1024;

    // 单双异步，默认单异步
    private boolean whenComplete = false;


    //	Http Async 参数选项：
    //	连接超时时间
    private int httpConnectTimeout = 30 * 1000;

    //	请求超时时间
    private int httpRequestTimeout = 5 * 1000;

    //	客户端请求重试次数
    private int httpMaxRequestRetry = 2;

    //	客户端请求最大连接数
    private int httpMaxConnections = 10000;

    //	客户端每个地址支持的最大连接数
    private int httpConnectionsPerHost = 8000;

    //	客户端空闲连接超时时间, 默认60秒
    private int httpPooledConnectionIdleTimeout = 60 * 1000;

    //缓冲区类型
    private String bufferType = "parallel22";

    //缓冲区大小
    private int bufferSize = 1024 * 16;

    //处理线程数
    private int processThread = Runtime.getRuntime().availableProcessors();

    //等待策略
    private String waitStrategy ="blocking";

    public WaitStrategy getWaitStrategy(){
        switch (waitStrategy){
            case "blocking":
                return new BlockingWaitStrategy();
            case "busySpin":
                return new BusySpinWaitStrategy();
            case "yielding":
                return new YieldingWaitStrategy();
            case "sleeping":
                return new SleepingWaitStrategy();
            default:
                return new BlockingWaitStrategy();
        }
    }

}
