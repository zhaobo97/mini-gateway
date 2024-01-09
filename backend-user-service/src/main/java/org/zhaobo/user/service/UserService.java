package org.zhaobo.user.service;

import org.zhaobo.user.dao.UserDao;
import org.zhaobo.user.model.MyUser;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class UserService {

    @Resource
    private UserDao userDao;

    public MyUser login(String nickname, String phoneNumer) {
        //这里需要校验一下验证码，一般可以把验证码放在redis
        return userDao.findByPhoneNumber(phoneNumer)
                .orElseGet(() -> userDao.save(MyUser.builder()
                        .nickname(nickname)
                        .phoneNumber(phoneNumer).build()));
    }

    public MyUser getUser(long userId) {
        return userDao.findById(userId).get();
    }
}
