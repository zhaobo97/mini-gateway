package org.zhaobo.user.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.zhaobo.user.dto.UserInfo;
import org.zhaobo.user.model.MyUser;
import org.zhaobo.user.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.zhaobo.client.core.ApiInvoker;
import org.zhaobo.client.core.ApiProtocol;
import org.zhaobo.client.core.ApiService;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Map;

@Slf4j
@RestController
@ApiService(serviceId = "backend-user-server", protocol = ApiProtocol.HTTP, patternPath = "/user/**")
public class UserController {
    private static final String SECRETKEY = "faewifheafewhefsfjkds";//一般不会直接写代码里，可以用一些安全机制来保护
    private static final String COOKIE_NAME = "user-jwt";

    @Resource
    private UserService userService;

    @ApiInvoker(path = "/login")
    @PostMapping("/login")
    public UserInfo login(@RequestBody Map<String, String> requestBody,
                          HttpServletResponse response) {
        String nickname = requestBody.get("nickname");
        String phoneNumber = requestBody.get("phoneNumber");
        MyUser user = userService.login(nickname,phoneNumber);
        String jwt = Jwts.builder()
            .setSubject(String.valueOf(user.getId()))
            .setIssuedAt(new Date())
            .signWith(SignatureAlgorithm.HS256, SECRETKEY).compact();
        response.addCookie(new Cookie(COOKIE_NAME, jwt));
        return UserInfo.builder()
            .id(user.getId())
            .nickname(user.getNickname())
            .phoneNumber(user.getPhoneNumber()).build();
    }

    @GetMapping("/private/user-info")
    public UserInfo getUserInfo(@RequestHeader("userId") String userId) {
        log.info("userId :{}", userId);
        MyUser user = userService.getUser(Long.parseLong(userId));
        return UserInfo.builder()
            .id(user.getId())
            .nickname(user.getNickname())
            .phoneNumber(user.getPhoneNumber()).build();
    }
}
