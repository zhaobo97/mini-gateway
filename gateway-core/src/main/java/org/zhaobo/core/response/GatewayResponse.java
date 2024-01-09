package org.zhaobo.core.response;


import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.*;
import lombok.Data;
import org.asynchttpclient.Response;
import org.zhaobo.common.enums.ResponseCode;
import org.zhaobo.common.utils.JSONUtil;

import java.util.Map;

/**
 * @Auther: bo
 * @Date: 2023/12/1 20:07
 * @Description:
 */
@Data
public class GatewayResponse {

    // 响应头
    private HttpHeaders responseHeaders = new DefaultHttpHeaders();

    // 额外的响应头
    private HttpHeaders extraResponseHeaders = new DefaultHttpHeaders();

    // 响应内容
    private String content;

    // 响应状态码
    private HttpResponseStatus httpResponseStatus;

    // 异步返回的对象
    private Response futureResponse;

    public GatewayResponse() {

    }

    /**
     * 设置响应头
     *
     * @param name
     * @param value
     */
    public void putHeaders(CharSequence name, CharSequence value) {
        this.responseHeaders.add(name, value);
    }

    /**
     * 构建GatewayResponse
     *
     * @param futureResponse
     * @return
     */
    public static GatewayResponse build(Response futureResponse) {
        GatewayResponse gatewayResponse = new GatewayResponse();
        gatewayResponse.setFutureResponse(futureResponse);
        gatewayResponse.setHttpResponseStatus(HttpResponseStatus.valueOf(futureResponse.getStatusCode()));
        return gatewayResponse;
    }

    /**
     * 返回一个 JSON 类型的响应，失败是使用
     *
     * @param code
     * @return
     */
    public static GatewayResponse buildOnFailure(ResponseCode code) {
        ObjectNode objectNode = JSONUtil.createObjectNode();
        objectNode.put(JSONUtil.CODE, code.getAppCode());
        objectNode.put(JSONUtil.STATUS, code.getHttpResponseStatus().code());
        objectNode.put(JSONUtil.MESSAGE, code.getMessage());

        GatewayResponse gatewayResponse = new GatewayResponse();
        gatewayResponse.putHeaders(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        gatewayResponse.setHttpResponseStatus(code.getHttpResponseStatus());
        gatewayResponse.setContent(JSONUtil.toJSONString(objectNode));
        return gatewayResponse;
    }

    public static GatewayResponse buildOnSuccess(Object data) {
        ObjectNode objectNode = JSONUtil.createObjectNode();
        GatewayResponse gatewayResponse = new GatewayResponse();
        if (data instanceof Response) {
            Response response = (Response) data;
            for (Map.Entry<String, String> header : response.getHeaders()) {
                gatewayResponse.putHeaders(header.getKey(), header.getValue());
            }
            objectNode.putPOJO(JSONUtil.DATA, response);
        } else {
            objectNode.putPOJO(JSONUtil.DATA, data);
        }

        gatewayResponse.putHeaders(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        gatewayResponse.setHttpResponseStatus(ResponseCode.SUCCESS.getHttpResponseStatus());
        gatewayResponse.setContent(JSONUtil.toJSONString(objectNode));
        return gatewayResponse;
    }


}
