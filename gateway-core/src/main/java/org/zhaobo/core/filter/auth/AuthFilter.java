package org.zhaobo.core.filter.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zhaobo.common.exception.NoCookieException;
import org.zhaobo.core.context.GatewayContext;
import org.zhaobo.common.constants.FilterConst;
import org.zhaobo.core.filter.Filter;
import org.zhaobo.core.filter.FilterInfo;

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
        try {
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
        } catch (Exception e) {
            ctx.setTerminated();
            if (e instanceof NoCookieException) {
                log.error("no cookie :{}" , e.getMessage());
                throw e;
            }else {
                e.printStackTrace();
                throw e;
            }
        }
    }

    private long parseToken(String token) {
        Claims body = (Claims) Jwts.parser().setSigningKey(SECRET_KEY).parse(token).getBody();
        return Long.parseLong(body.getSubject());
    }
}
