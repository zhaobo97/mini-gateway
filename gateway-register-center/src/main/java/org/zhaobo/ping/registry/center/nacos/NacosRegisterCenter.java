package org.zhaobo.ping.registry.center.nacos;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingMaintainFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.common.utils.CollectionUtils;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.zhaobo.ping.common.config.ServiceDefinition;
import org.zhaobo.ping.common.config.ServiceInstance;
import org.zhaobo.ping.common.constants.GatewayConst;
import org.zhaobo.ping.registry.center.api.RegisterCenter;
import org.zhaobo.ping.registry.center.api.RegisterCenterListener;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Auther: bo
 * @Date: 2023/12/3 18:07
 * @Description:
 */
@Slf4j
public class NacosRegisterCenter implements RegisterCenter {

    private String registerAddress;

    private String namespace;

    private String group;

    //获取全部服务实例的NamingService
    private NamingService allNamingServer;

    // 维护服务实例信息
    private NamingService namingService;

    // 维护服务定义信息
    private NamingMaintainService namingMaintainService;

    // 监听器列表
    private final List<RegisterCenterListener> registerCenterListenerList = new CopyOnWriteArrayList<>();


    @Override
    public void init(String registerAddress,String namespace, String group, String username, String password) {
        this.registerAddress = registerAddress;
        this.namespace = namespace;
        this.group = group;

        Properties properties = new Properties();
        //设置注册地址
        properties.setProperty(PropertyKeyConst.SERVER_ADDR,registerAddress);
        //设置命名空间
        properties.setProperty(PropertyKeyConst.NAMESPACE,namespace);
        properties.setProperty(PropertyKeyConst.USERNAME,username);
        properties.setProperty(PropertyKeyConst.PASSWORD,password);

        try {
            this.namingMaintainService = NamingMaintainFactory.createMaintainService(properties);
            this.namingService = NamingFactory.createNamingService(properties);
            this.allNamingServer = NamingFactory.createNamingService(registerAddress);

        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        // 构造Nacos实例信息
        Instance instance = new Instance();
        instance.setInstanceId(serviceInstance.getIp());
        instance.setIp(serviceInstance.getIp());
        instance.setPort(serviceInstance.getPort());
        instance.setMetadata(Map.of(GatewayConst.META_DATA_KEY,
                JSON.toJSONString(serviceInstance)));

        try {
            // 注册
            namingService.registerInstance(serviceDefinition.getServiceId(), group, instance);

            // 更新服务定义
            namingMaintainService.updateService(serviceDefinition.getServiceId(), group, 0,
                    Map.of(GatewayConst.META_DATA_KEY, JSON.toJSONString(serviceDefinition)));
            log.info("register {} {}", serviceDefinition, serviceInstance);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            namingService.deregisterInstance(serviceDefinition.getServiceId(), group,
                    serviceInstance.getIp(), serviceInstance.getPort());
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeAllServices(RegisterCenterListener listener) {
        registerCenterListenerList.add(listener);
        doSubscribeAllServices();

        //可能有新服务加入，所以需要有一个定时任务来检查
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1,
                new DefaultThreadFactory("doSubscribeAllServices-thread"));
        pool.scheduleWithFixedDelay(this::doSubscribeAllServices,
                10, 10, TimeUnit.SECONDS);
    }

    private void doSubscribeAllServices() {
        // 获取已经订阅的服务
        try {
            Set<String> subsecriptService = namingService.getSubscribeServices()
                    .stream().map(ServiceInfo::getName).collect(Collectors.toSet());
            int pageNo = 1;
            int pageSize = 100;

            // 分页从nacos上拿到服务列表
            List<String> serviceList = namingService.getServicesOfServer(pageNo, pageSize, group).getData();
            // 把nacos服务器上没订阅的订阅了
            while (CollectionUtils.isNotEmpty(serviceList)) {
                log.info("service list size {}", serviceList.size());

                for (String service : serviceList) {
                    if (subsecriptService.contains(service)) {
                        continue;
                    }
                    // nacos事件监听器, 不包含：触发事件
                    // 只有第一次订阅才会走这个逻辑，执行onEvent
                    // 订阅过的通过nacos客户端内部的定时任务每6s获取实例列表，
                    // 判断实例是否变化，并处理变化，其中就包括调用listener
                    EventListener eventListener = new NacosRegisterListener();
                    eventListener.onEvent(new NamingEvent(service, null));
                    namingService.subscribe(service, group, eventListener);
                    log.info("subscribe {} {}", service, group);
                }
                // 分页查下一页的
                serviceList = namingService.getServicesOfServer(++pageNo, pageSize, group).getData();
            }
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    private class NacosRegisterListener implements EventListener {
        @Override
        public void onEvent(Event event) {
            if (event instanceof NamingEvent) {
                NamingEvent namingEvent = (NamingEvent) event;
                String serviceName = namingEvent.getServiceName();
                try {
                    // 获取服务定义信息
                    Service service = namingMaintainService.queryService(serviceName, group);

                    ServiceDefinition serviceDefinition = JSON.parseObject(service.getMetadata()
                            .get(GatewayConst.META_DATA_KEY), ServiceDefinition.class);

                    // 获取服务实例信息
                    List<Instance> instanceList = namingService.getAllInstances(serviceName, group);

                    HashSet<ServiceInstance> set = new HashSet<>();

                    for (Instance instance : instanceList) {
                        ServiceInstance serviceInstance = JSON.parseObject(instance.getMetadata()
                                .get(GatewayConst.META_DATA_KEY), ServiceInstance.class);
                        set.add(serviceInstance);
                    }
                    // 刷新所有服务的本地缓存
                    registerCenterListenerList.forEach(listener ->
                            listener.onChange(serviceDefinition, set));

                } catch (NacosException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
