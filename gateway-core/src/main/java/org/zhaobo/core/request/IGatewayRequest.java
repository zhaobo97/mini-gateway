package org.zhaobo.core.request;

import io.netty.handler.codec.http.Cookie;
import org.asynchttpclient.Request;

/**
 * @Auther: bo
 * @Date: 2023/11/30 17:00
 * @Description:
 */
public interface IGatewayRequest {

    // 设置目标地址
    void setModifyHost(String host);

    // 获取目标地址
    String getModifyHost();

    // 设置服务路径
    void setModifyPath(String path);

    // 获取服务路径
    String getModifyPath();

    // 添加请求头
    void addHeader(CharSequence name, String value);

    // 设置请求头
    void setHeader(CharSequence name, String value);

    // 添加get请求参数
    void addQueryParam(String name, String value);

    // 添加post请求
    void addFormParam(String name, String value);

    // 添加或者替换cookie
    void addOrReplaceCookie(Cookie cookie);

    // 设置超时时间
    void setRequestTimeout(int timeout);

    // 获取最终请求路径
    String getFinalUrl();

    // 构造最终的请求对象
    Request build();




}
