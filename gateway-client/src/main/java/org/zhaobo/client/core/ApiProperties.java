package org.zhaobo.client.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Auther: bo
 * @Date: 2023/12/4 16:17
 * @Description:
 */
@Data
@ConfigurationProperties(prefix = "api")
public class ApiProperties {

    private String registerAddress;

    //空格代表的就是public的命名空间id
    private String namespace = "";

    private String group = "DEFAULT_GROUP";

    private String username;

    private String password;

    private boolean gray;
}
