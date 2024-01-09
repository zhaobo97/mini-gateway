package org.zhaobo.client.core.autoconfigure;

import org.apache.dubbo.config.spring.ServiceBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.zhaobo.client.core.ApiProperties;
import org.zhaobo.client.support.dubbo.DubboRegisterManager;
import org.zhaobo.client.support.springmvc.SpringMvcRegisterManager;

import javax.annotation.Resource;
import javax.servlet.Servlet;

/**
 * @Auther: bo
 * @Date: 2023/12/4 20:44
 * @Description: Springboot 自动装配
 */
@Configuration
@EnableConfigurationProperties(ApiProperties.class)
@ConditionalOnProperty(value = "api.registerAddress")
public class ApiClientAutoConfiguration {

    @Resource
    private ApiProperties apiProperties;

    @Bean
    @ConditionalOnClass({Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class}) // 防止重复扫描
    @ConditionalOnMissingBean(SpringMvcRegisterManager.class) // 防止重复注册
    public SpringMvcRegisterManager springMvcRegisterManager(){
        return new SpringMvcRegisterManager(apiProperties);
    }

    @Bean
    @ConditionalOnClass({ServiceBean.class})
    @ConditionalOnMissingBean(DubboRegisterManager.class)
    public DubboRegisterManager dubboRegisterManager(){
        return new DubboRegisterManager(apiProperties);
    }


}
