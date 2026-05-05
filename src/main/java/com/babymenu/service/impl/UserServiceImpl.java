package com.babymenu.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.babymenu.common.BizException;
import com.babymenu.dto.LoginDTO;
import com.babymenu.dto.LoginVO;
import com.babymenu.entity.User;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.UserService;
import com.babymenu.util.JwtUtil;
import com.babymenu.util.UserContext;
import com.babymenu.wechat.WechatLoginResp;
import com.babymenu.wechat.WechatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final WechatService wechatService;
    private final JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginDTO dto) {
        if (dto.getCode() == null || dto.getCode().isBlank()) {
            throw new BizException("缺少 code");
        }
        WechatLoginResp resp = wechatService.code2Session(dto.getCode());
        String openid = resp.getOpenid();

        User user = getOne(Wrappers.<User>lambdaQuery().eq(User::getOpenid, openid));
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setUnionid(resp.getUnionid());
            user.setNickname(dto.getNickname() == null ? "宝贝" : dto.getNickname());
            user.setAvatar(dto.getAvatar());
            user.setGender(dto.getGender() == null ? 0 : dto.getGender());
            user.setDailyPointsLimit(50);
            user.setPoints(50);
            save(user);
        } else {
            boolean change = false;
            if (dto.getNickname() != null) { user.setNickname(dto.getNickname()); change = true; }
            if (dto.getAvatar()   != null) { user.setAvatar(dto.getAvatar());     change = true; }
            if (dto.getGender()   != null) { user.setGender(dto.getGender());     change = true; }
            if (change) updateById(user);
        }

        String token = jwtUtil.createToken(user.getId(), openid);
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUser(user);
        vo.setBound(user.getCoupleId() != null);
        return vo;
    }

    @Override
    public User currentUser() {
        Long uid = UserContext.get();
        if (uid == null) throw new BizException(401, "未登录");
        return getById(uid);
    }

    @Override
    public void clearUnreadReward() {
        Long uid = UserContext.get();
        if (uid != null) {
            User user = getById(uid);
            if (user != null && user.getHasUnreadReward() != null && user.getHasUnreadReward() > 0) {
                user.setHasUnreadReward(0);
                updateById(user);
            }
        }
    }
    @Override
    public void updateProfile(String nickname, String avatar) {
        Long uid = UserContext.get();
        if (uid == null) throw new BizException(401, "未登录");
        User user = getById(uid);
        if (user == null) throw new BizException("用户不存在");

        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
        }
        if (avatar != null && !avatar.isBlank()) {
            user.setAvatar(avatar);
        }
        updateById(user);
    }
}
