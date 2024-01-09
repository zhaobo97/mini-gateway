package org.zhaobo.ping.core;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.zhaobo.ping.common.config.DynamicConfigManager;
import org.zhaobo.ping.common.config.ServiceDefinition;
import org.zhaobo.ping.common.config.ServiceInstance;
import org.zhaobo.ping.common.utils.NetUtils;
import org.zhaobo.ping.common.utils.TimeUtil;
import org.zhaobo.ping.config.center.api.ConfigCenter;
import org.zhaobo.ping.common.constants.BasicConst;
import org.zhaobo.ping.registry.center.api.RegisterCenter;
import org.zhaobo.ping.registry.center.api.RegisterCenterListener;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

@Slf4j
public class Bootstrap {
    public static void main(String[] args) {
        // 加载网关静态配置
        Config config = ConfigLoader.getInstance().load(args);

        // 初始化插件
        // 配置中心管理器初始化，连接配置中心：监听配置的新增、修改、删除
        ConfigCenter configCenter = loadConfigCenter();
        configCenter.init(config.getRegistryAddress(), config.getNamespace(), config.getGroup(), config.getUsername(), config.getPassword());
        configCenter.subscribeRulesChange(rules -> DynamicConfigManager.getInstance().putAllRule(rules));

        // 启动容器
        Container container = new Container(config);
        container.start();

        // 连接注册中心：将注册中心的实例加载到本地
        RegisterCenter registerCenter = registerAndSubscribe(config);

        log.info("gateway started at port:{}", config.getPort());

        // 服务优雅关机，收到 kill 信号时调用
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    registerCenter.deregister(buildGatewayServiceDefinition(config),
                            buildGatewayServiceInstance(config));
                    container.shutdown();
        }));
    }

    private static ConfigCenter loadConfigCenter() {
        ServiceLoader<ConfigCenter> serviceLoader = ServiceLoader.load(ConfigCenter.class);
        return serviceLoader.findFirst().orElseThrow(() -> {
            log.error("ConfigCenter impl not found!");
            return new RuntimeException("ConfigCenter impl not found!");
        });
    }

    private static RegisterCenter registerAndSubscribe(Config config) {
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        RegisterCenter registerCenter = serviceLoader.findFirst()
                .orElseThrow(() -> {
                    log.error("RegisterCenter impl not found!");
                    return new RuntimeException("RegisterCenter impl not found!");
                });
        registerCenter.init(config.getRegistryAddress(), config.getNamespace(), config.getGroup(), config.getUsername(), config.getPassword());
        // 构建网关的服务定义和服务实例
        ServiceDefinition serviceDefinition = buildGatewayServiceDefinition(config);
        ServiceInstance serviceInstance = buildGatewayServiceInstance(config);
        // 注册
        registerCenter.register(serviceDefinition, serviceInstance);
        // 订阅
        registerCenter.subscribeAllServices(new RegisterCenterListener() {
            @Override
            public void onChange(ServiceDefinition serviceDefinition, Set<ServiceInstance> serviceInstanceSet) {
                DynamicConfigManager manager = DynamicConfigManager.getInstance();
                manager.addServiceInstance(serviceDefinition.getUniqueId(), serviceInstanceSet);
                manager.putServiceDefinition(serviceDefinition.getUniqueId(), serviceDefinition);
                log.info("refresh service and instance : {} {}", serviceInstance.getUniqueId(),
                        JSON.toJSON(serviceInstanceSet));
            }
        });
        return registerCenter;
    }

    private static ServiceInstance buildGatewayServiceInstance(Config config) {
        String localIp = NetUtils.getLocalIp();
        int port = config.getPort();
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setServiceInstanceId(localIp + BasicConst.COLON_SEPARATOR + port);
        serviceInstance.setIp(localIp);
        serviceInstance.setPort(port);
        serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
        return serviceInstance;
    }

    private static ServiceDefinition buildGatewayServiceDefinition(Config config) {
        ServiceDefinition serviceDefinition = new ServiceDefinition();
        serviceDefinition.setInvokerMap(Map.of());
        serviceDefinition.setUniqueId(config.getApplicationName());
        serviceDefinition.setServiceId(config.getApplicationName());
        serviceDefinition.setNamespace(config.getNamespace());
        serviceDefinition.setGroup(config.getGroup());
        return serviceDefinition;
    }
}
