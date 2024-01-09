package org.zhaobo.user.rpc;

import lombok.RequiredArgsConstructor;
import org.zhaobo.user.dto.UserInfo;
import org.zhaobo.user.model.MyUser;
import org.zhaobo.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class UserRpcController {
    private final UserService userService;

    @GetMapping("/user/rpc/users/{userId}/info")
    public UserInfo getUserInfo(@PathVariable("userId") long userId) {
        MyUser user = userService.getUser(userId);
        return UserInfo.builder()
            .id(user.getId())
            .phoneNumber(user.getPhoneNumber())
            .nickname(user.getNickname())
            .build();
    }
}
