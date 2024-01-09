package org.zhaobo.ping.core.filter.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zhaobo.ping.common.constants.FilterConst;
import org.zhaobo.ping.core.context.GatewayContext;
import org.zhaobo.ping.core.filter.Filter;
import org.zhaobo.ping.core.filter.FilterInfo;

/**
 * @Auther: bo
 * @Date: 2023/12/8 21:51
 * @Description:
 */

@Slf4j
@FilterInfo(id = FilterConst.AUTH_FILTER_ID,
        name = FilterConst.AUTH_FILTER_NAME,
        order = FilterConst.AUTH_FILTER_ORDER)
public class AuthFilter implements Filter {
    private static final String SECRET_KEY = "zhaobo";
    private static final String USER_NAME = "user-jwt";

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // 是否需要鉴权
        if (ctx.getRule().getFilterConfig(FilterConst.AUTH_FILTER_ID) == null){
            return;
        }
        // 获取token
        String token = ctx.getRequest().getCookie(USER_NAME).value();
        if (StringUtils.isEmpty(token)) {
            return;
        }
        // 解析token
        long userId = parseToken(token);
        // 传到下游
        ctx.getRequest().setUserId(userId);
    }

    private long parseToken(String token) {
        Claims body = (Claims) Jwts.parser().setSigningKey(SECRET_KEY).parse(token).getBody();
        return Long.parseLong(body.getSubject());
    }
}
