package org.zhaobo.core.request;

import com.alibaba.fastjson.JSONPath;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import lombok.Setter;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.internal.StringUtil;
import lombok.Getter;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.zhaobo.common.constants.BasicConst;
import org.zhaobo.common.enums.ResponseCode;
import org.zhaobo.common.exception.NoCookieException;

import java.nio.charset.Charset;
import java.util.*;

/**
 * @Auther: bo
 * @Date: 2023/12/1 10:51
 * @Description: 基本上网络请求都是用的netty包的类，构建下游http请求用的是async-http-client
 */

@Getter
public class GatewayRequest implements IGatewayRequest {

    // 服务唯一ID
    private final String uniqueId;

    // 进入网关的开始时间
    private final long beginTime;

    // 字符集
    private final Charset charset;

    // 客户端ip
    private final String clientIP;

    // 服务端主机名
    private final String host;

    // 服务端请求路径（不包括请求参数）
    private final String path;

    // 统一资源标识符 xxx/xxx?attr1=a&attr=2
    private final String uri;

    // 请求方式 POST / GET / PUT
    private final HttpMethod httpMethod;

    // 请求格式 contentType
    private final String contentType;

    // 请求头
    private final HttpHeaders httpHeaders;

    // 参数解析器
    private final QueryStringDecoder queryStringDecoder;

    // fullHttpRequest
    private final FullHttpRequest fullHttpRequest;

    // 请求体
    private String body;

    // cookie
    private Map<String, io.netty.handler.codec.http.cookie.Cookie> cookieMap;

    // post 请求参数
    private Map<String, List<String>> parameters;

    /**
     * 可修改参数
     * 可修改schema，默认为http://
     */
    private String modifySchema;

    // 可修改主机
    private String modifyHost;

    // 可修改服务端路径
    private String modifyPath;

    @Setter
    // user id
    private long userId;

    // 构建下游请求时的http构建器
    private final RequestBuilder requestBuilder;

    public GatewayRequest(String uniqueId,
                          Charset charset,
                          String clientIP,
                          String host,
                          String uri,
                          String path,
                          HttpMethod httpMethod,
                          Map<String, List<String>> parameters,
                          String body,
                          String contentType,
                          HttpHeaders httpHeaders,
                          FullHttpRequest fullHttpRequest,
                          RequestBuilder requestBuilder) {
        this.uniqueId = uniqueId;
        this.beginTime = System.currentTimeMillis();
        this.charset = charset;
        this.clientIP = clientIP;
        this.host = host;
        this.path = path;
        this.uri = uri;
        this.httpMethod = httpMethod;
        this.contentType = contentType;
        this.httpHeaders = httpHeaders;
        this.queryStringDecoder = new QueryStringDecoder(uri, charset);
        this.fullHttpRequest = fullHttpRequest;
        this.body = body;
        this.parameters = parameters;
        this.requestBuilder = requestBuilder;

        // 可变参数
        this.modifyHost = host;
        this.modifyPath = path;
        this.modifySchema = BasicConst.HTTP_PREFIX_SEPARATOR;
        this.requestBuilder.setMethod(getHttpMethod().name());
        this.requestBuilder.setHeaders(getHttpHeaders());
        this.requestBuilder.setQueryParams(queryStringDecoder.parameters());

        ByteBuf byteBuf = fullHttpRequest.content();
        if (Objects.nonNull(byteBuf)) {
            this.requestBuilder.setBody(byteBuf.nioBuffer());
        }

    }

    /**
     * 获取body
     *
     * @return
     */
    public String getBody() {
        if (StringUtil.isNullOrEmpty(body)) {
            body = fullHttpRequest.content().toString();
        }
        return body;
    }

    /**
     * 获取cookie
     *
     * @return
     */
    public io.netty.handler.codec.http.cookie.Cookie getCookie(String key) {
        if (Objects.isNull(cookieMap)) {
            cookieMap = new HashMap<>();
            String cookieKey = getHttpHeaders().get(HttpHeaderNames.COOKIE);
            if (Objects.isNull(cookieKey))
                throw new NoCookieException(ResponseCode.UNAUTHORIZED.getMessage(), ResponseCode.UNAUTHORIZED);
            Set<io.netty.handler.codec.http.cookie.Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieKey);
            for (io.netty.handler.codec.http.cookie.Cookie cookie : cookies) {
                cookieMap.put(key, cookie);
            }
        }
        return cookieMap.get(key);
    }

    /**
     * 获取请求参数
     *
     * @param name
     */
    public List<String> getParameters(String name) {
        return queryStringDecoder.parameters().get(name);
    }

    public List<String> getPostParameters(String name) {
        String body = getBody();
        if (isFormPost()) {
            if (parameters == null) {
                QueryStringDecoder decoder = new QueryStringDecoder(body, false);
                parameters = decoder.parameters();
            }
            if (parameters.isEmpty()) return null;
            else return parameters.get(name);
        } else if (isJsonPost()) {
            return Collections.singletonList(JSONPath.read(body, name).toString());
        }
        return null;
    }

    private boolean isJsonPost() {
        return HttpMethod.POST.equals(httpMethod) &&
                contentType.startsWith(HttpHeaderValues.APPLICATION_JSON.toString());
    }

    private boolean isFormPost() {
        return HttpMethod.POST.equals(httpMethod) &&
                (contentType.startsWith(HttpHeaderValues.FORM_DATA.toString()) ||
                        contentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString()));

    }


    @Override
    public void setModifyHost(String host) {
        this.modifyHost = host;
    }

    @Override
    public String getModifyHost() {
        return this.modifyHost;
    }

    @Override
    public void setModifyPath(String path) {
        this.modifyPath = path;
    }

    @Override
    public String getModifyPath() {
        return modifyPath;
    }

    @Override
    public void addHeader(CharSequence name, String value) {
        this.requestBuilder.addHeader(name, value);
    }

    @Override
    public void setHeader(CharSequence name, String value) {
        this.requestBuilder.setHeader(name, value);
    }

    @Override
    public void addQueryParam(String name, String value) {
        this.requestBuilder.addQueryParam(name, value);
    }

    @Override
    public void addFormParam(String name, String value) {
        this.requestBuilder.addFormParam(name, value);
    }

    @Override
    public void addOrReplaceCookie(Cookie cookie) {
        this.requestBuilder.addOrReplaceCookie(cookie);
    }

    @Override
    public void setRequestTimeout(int timeout) {
        this.requestBuilder.setRequestTimeout(timeout);
    }

    @Override
    public String getFinalUrl() {
        return modifySchema + modifyHost + modifyPath;
    }

    @Override
    public Request build() {
        requestBuilder.setUrl(getFinalUrl());
        requestBuilder.setHeader("userId", String.valueOf(userId));
        return requestBuilder.build();
    }
}
