package org.zhaobo.client.core;

/**
 * @Auther: bo
 * @Date: 2023/12/4 11:05
 * @Description:
 */
public enum ApiProtocol {
    HTTP("http","http协议"),
    DUBBO("dubbo","dubbo协议");

    private String code;

    private String desc;

    ApiProtocol(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
