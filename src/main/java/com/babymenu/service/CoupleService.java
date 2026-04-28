package com.babymenu.service;

import com.babymenu.entity.Couple;
import com.babymenu.entity.User;

public interface CoupleService {
    /** 生成邀请码 */
    String generateInvite();

    /** 通过邀请码绑定 */
    Couple bindByCode(String code);

    /** 解绑 */
    void unbind();

    /** 获取伴侣信息 */
    User partner();
}
