package org.zhaobo.ping.common.config;

import lombok.Getter;
import lombok.Setter;

/**
 * @Auther: bo
 * @Date: 2023/12/4 11:35
 * @Description:
 */
@Getter
@Setter
public class DubboServiceInvoker extends AbstractServiceInvoker{

    private String registerAddress;

    private String interfaceClass;

    private String methodName;

    private String[] parameterTypes;

    private String version;

}
