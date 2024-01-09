package org.zhaobo.user.dao;

import org.zhaobo.user.model.MyUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public interface UserDao extends CrudRepository<MyUser, Long> {
    Optional<MyUser> findByPhoneNumber(String phoneNumber);
}
