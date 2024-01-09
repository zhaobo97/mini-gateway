package org.zhaobo.core.helper;

import com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.CharsetUtil;
import org.zhaobo.common.config.*;
import org.zhaobo.core.context.GatewayContext;
import org.zhaobo.common.constants.BasicConst;
import org.zhaobo.common.constants.GatewayConst;
import org.zhaobo.common.exception.ResponseException;
import org.zhaobo.common.utils.AssertUtil;
import org.zhaobo.core.request.GatewayRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.RequestBuilder;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static org.zhaobo.common.enums.ResponseCode.PATH_NO_MATCHED;

public class RequestHelper {

	public static GatewayContext doContext(FullHttpRequest fullHttpRequest, ChannelHandlerContext channelContext) {
		
		//	构建请求对象GatewayRequest
		GatewayRequest gateWayRequest = doRequest(fullHttpRequest, channelContext);
		
		//	根据请求对象里的uniqueId，获取资源服务信息(也就是服务定义信息)
		ServiceDefinition serviceDefinition = DynamicConfigManager.getInstance()
				.getServiceDefinition(gateWayRequest.getUniqueId());
		AssertUtil.notNull(serviceDefinition, "serviceDefinition is null，please check service status！");
		// 可能是没用的代码
		//	根据请求对象获取服务定义对应的方法调用，然后获取对应的规则
		ServiceInvoker serviceInvoker = new HttpServiceInvoker();
		serviceInvoker.setInvokerPath(gateWayRequest.getPath());
		serviceInvoker.setTimeout(500);

		//根据请求对象获取规则
		Rule rule = getRule(gateWayRequest,serviceDefinition.getServiceId());

		//	构建我们而定GateWayContext对象
		GatewayContext gatewayContext = GatewayContext.builder()
				.protocol(serviceDefinition.getProtocol())
				.nettyCtx(channelContext)
				.keepalive(HttpUtil.isKeepAlive(fullHttpRequest))
				.request(gateWayRequest)
				.rule(rule)
				.build();

		return gatewayContext;
	}
	
	/**
	 * 构建GatewayRequest请求对象
	 */
	private static GatewayRequest doRequest(FullHttpRequest fullHttpRequest, ChannelHandlerContext channelContext) {
		
		HttpHeaders headers = fullHttpRequest.headers();
		//	从header头获取必须要传入的关键属性 uniqueId, 全局唯一的请求id
		String uniqueId = headers.get(GatewayConst.UNIQUE_ID);

		String host = headers.get(HttpHeaderNames.HOST);
		HttpMethod method = fullHttpRequest.method();
		String methodName = method.name();
		String uri = fullHttpRequest.uri();
		String clientIp = getClientIp(channelContext, fullHttpRequest);
		String contentType = HttpUtil.getMimeType(fullHttpRequest) == null ? null : HttpUtil.getMimeType(fullHttpRequest).toString();
		Charset charset = HttpUtil.getCharset(fullHttpRequest, StandardCharsets.UTF_8);

		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri, charset);
		String path = queryStringDecoder.path();

		RequestBuilder requestBuilder = new RequestBuilder();
		requestBuilder.setHeaders(headers);
		requestBuilder.setMethod(methodName);

		Map<String, List<String>> parameters = null;
		String body = null;
		if (methodName == "GET"){
			parameters = queryStringDecoder.parameters();
			requestBuilder.setQueryParams(parameters);
		}else if (methodName == "POST"){
			body = fullHttpRequest.content().toString(CharsetUtil.UTF_8);
			System.out.println(body);
			requestBuilder.setBody(body);
			requestBuilder.setHeader("Content-Type", "application/json");
		}

		GatewayRequest gatewayRequest = new GatewayRequest(uniqueId,
				charset,
				clientIp,
				host, 
				uri,
				path,
				method,
				parameters,
				body,
				contentType,
				headers,
				fullHttpRequest,
				requestBuilder);
		
		return gatewayRequest;
	}
	
	/**
	 * 获取客户端ip
	 */
	private static String getClientIp(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
		String xForwardedValue = fullHttpRequest.headers().get(BasicConst.HTTP_FORWARD_SEPARATOR);
		
		String clientIp = null;
		if(StringUtils.isNotEmpty(xForwardedValue)) {
			List<String> values = Arrays.asList(xForwardedValue.split(", "));
			if(values.size() >= 1 && StringUtils.isNotBlank(values.get(0))) {
				clientIp = values.get(0);
			}
		}
		if(clientIp == null) {
			InetSocketAddress inetSocketAddress = (InetSocketAddress)ctx.channel().remoteAddress();
			clientIp = inetSocketAddress.getAddress().getHostAddress();
		}
		return clientIp;
	}

	/**
	 * 获取Rule对象
	 * @param gateWayRequest
	 * @return
	 */
	private static Rule getRule(GatewayRequest gateWayRequest,String serviceId){
       String key = serviceId + "." + gateWayRequest.getPath();
	   Rule rule = DynamicConfigManager.getInstance().getRuleByPath(key);

	   if (rule != null){
		   return rule;
	   }
	   return DynamicConfigManager.getInstance().getRuleByServiceId(serviceId)
			   .stream().filter(r -> gateWayRequest.getPath().startsWith(r.getPrefix()))
			   .findAny().orElseThrow(()-> new ResponseException(PATH_NO_MATCHED));
	}
}
