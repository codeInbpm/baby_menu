package com.babymenu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.babymenu.dto.LoginDTO;
import com.babymenu.dto.LoginVO;
import com.babymenu.entity.User;

public interface UserService extends IService<User> {
    LoginVO login(LoginDTO dto);
    User currentUser();
    void clearUnreadReward();
}
