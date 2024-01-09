package org.zhaobo.client.support.dubbo;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.context.event.ServiceBeanExportedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.zhaobo.client.core.ApiProperties;
import org.zhaobo.client.core.ApiAnnotationScanner;
import org.zhaobo.client.support.AbstractClientRegisterManager;
import org.zhaobo.ping.common.config.ServiceDefinition;
import org.zhaobo.ping.common.config.ServiceInstance;
import org.zhaobo.ping.common.constants.BasicConst;
import org.zhaobo.ping.common.constants.GatewayConst;
import org.zhaobo.ping.common.utils.NetUtils;
import org.zhaobo.ping.common.utils.TimeUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * @Auther: bo
 * @Date: 2023/12/4 19:35
 * @Description: 仅适用于dubbo 2.7
 */
@Slf4j
public class DubboRegisterManager extends AbstractClientRegisterManager
        implements ApplicationListener<ApplicationEvent> {

    // 保存处理过的bean
    private Set<Object> set = new HashSet<>();

    public DubboRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ServiceBeanExportedEvent){
            try {
                ServiceBeanExportedEvent event = (ServiceBeanExportedEvent) applicationEvent;
                ServiceBean<?> serviceBean = event.getServiceBean();
                doRegisterDubbo(serviceBean);
            } catch (Exception e){
                throw new RuntimeException("doRegisterDubbo error",e);
            }
        } else if (applicationEvent instanceof ApplicationStartedEvent){
            log.info("dubbo api has started");
        }
    }

    private void doRegisterDubbo(ServiceBean<?> serviceBean) {
        Object bean = serviceBean.getRef();
        if (set.contains(bean)){
            return;
        }
        ServiceDefinition serviceDefinition = ApiAnnotationScanner
                .getInstance().scanner(bean, serviceBean);
        if (serviceDefinition == null) {
            return;
        }
        // 服务定义赋值
        serviceDefinition.setGroup(getApiProperties().getGroup());
        serviceDefinition.setNamespace(getApiProperties().getNamespace());

        // 获取服务实例属性
        String localIp = NetUtils.getLocalIp();
        Integer port = serviceBean.getProtocol().getPort();
        String serviceIntanceId = localIp + BasicConst.COLON_SEPARATOR + port;
        String uniqueId = serviceDefinition.getUniqueId();
        String version = serviceDefinition.getVersion();

        // 服务实例赋值
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setIp(localIp);
        serviceInstance.setPort(port);
        serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
        serviceInstance.setUniqueId(uniqueId);
        serviceInstance.setVersion(version);
        serviceInstance.setServiceInstanceId(serviceIntanceId);
        serviceInstance.setWeight(GatewayConst.DEFAULT_WEIGHT);
        if (getApiProperties().isGray()) {
            serviceInstance.setGray(true);
        }
        // 注册
        register(serviceDefinition, serviceInstance);


    }
}
