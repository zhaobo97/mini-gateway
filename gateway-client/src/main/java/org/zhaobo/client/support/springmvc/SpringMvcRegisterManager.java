package org.zhaobo.client.support.springmvc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.zhaobo.client.core.ApiAnnotationScanner;
import org.zhaobo.client.core.ApiProperties;
import org.zhaobo.client.support.AbstractClientRegisterManager;
import org.zhaobo.common.config.ServiceDefinition;
import org.zhaobo.common.config.ServiceInstance;
import org.zhaobo.common.constants.BasicConst;
import org.zhaobo.common.constants.GatewayConst;
import org.zhaobo.common.utils.NetUtils;
import org.zhaobo.common.utils.TimeUtil;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Auther: bo
 * @Date: 2023/12/4 16:49
 * @Description:
 */
@Slf4j
public class SpringMvcRegisterManager extends AbstractClientRegisterManager
        implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Resource
    private ServerProperties serverProperties;

    private Set<Object> set = new HashSet<>();

    public SpringMvcRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ApplicationStartedEvent) {
            try {
                doRegisterSpringMvc();
            } catch (Exception e) {
                log.error("doRegisterSpringMvc error", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void doRegisterSpringMvc() {
        // 通过这个方法可以获取实现了RequestMappingHandlerMapping这个接口的类实例
        Map<String, RequestMappingHandlerMapping> requestMappings = BeanFactoryUtils
                .beansOfTypeIncludingAncestors(applicationContext,
                RequestMappingHandlerMapping.class, true, false);

        for (RequestMappingHandlerMapping value : requestMappings.values()) {
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = value.getHandlerMethods();

            for (HandlerMethod handlerMethod : handlerMethods.values()) {

                Class<?> clazz = handlerMethod.getBeanType();
                Object bean = applicationContext.getBean(clazz);

                if (set.contains(bean)) {
                    continue;
                }

                ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scanner(bean);

                if (serviceDefinition == null) {
                    continue;
                }

                // 服务定义赋值
                serviceDefinition.setNamespace(getApiProperties().getNamespace());
                serviceDefinition.setGroup(getApiProperties().getGroup());

                // 获取服务实例属性
                String localIp = NetUtils.getLocalIp();
                Integer port = serverProperties.getPort();
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

                // 注册前检查服务实例是否为灰度
                if (getApiProperties().isGray()) {
                    serviceInstance.setGray(true);
                }
                // 注册
                register(serviceDefinition, serviceInstance);
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
