package com.babymenu.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.babymenu.common.BizException;
import com.babymenu.entity.Couple;
import com.babymenu.entity.CoupleInvite;
import com.babymenu.entity.MenuCategory;
import com.babymenu.entity.User;
import com.babymenu.mapper.CoupleInviteMapper;
import com.babymenu.mapper.CoupleMapper;
import com.babymenu.mapper.MenuCategoryMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.CoupleService;
import com.babymenu.util.UserContext;
import com.babymenu.wechat.WechatSubscribeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoupleServiceImpl implements CoupleService {

    private final CoupleInviteMapper inviteMapper;
    private final CoupleMapper coupleMapper;
    private final UserMapper userMapper;
    private final MenuCategoryMapper categoryMapper;
    private final WechatSubscribeService subscribeService;

    @Override
    public String generateInvite() {
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null) throw new BizException("用户不存在");
        if (self.getCoupleId() != null) throw new BizException("你已经绑定了伴侣");

        String code = null;
        for (int i = 0; i < 3; i++) {
            String c = RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER, 8).toUpperCase();
            Long exist = inviteMapper.selectCount(
                    Wrappers.<CoupleInvite>lambdaQuery().eq(CoupleInvite::getInviteCode, c));
            if (exist == 0) { code = c; break; }
        }
        if (code == null) throw new BizException("邀请码生成失败，请重试");

        CoupleInvite invite = new CoupleInvite();
        invite.setInviterId(uid);
        invite.setInviteCode(code);
        invite.setExpireTime(LocalDateTime.now().plusDays(7));
        invite.setStatus(0);
        inviteMapper.insert(invite);
        return code;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Couple bindByCode(String code) {
        if (code == null || code.isBlank()) throw new BizException("请输入邀请码");
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self.getCoupleId() != null) throw new BizException("你已经绑定了伴侣");

        CoupleInvite invite = inviteMapper.selectOne(
                Wrappers.<CoupleInvite>lambdaQuery().eq(CoupleInvite::getInviteCode, code.trim().toUpperCase()));
        if (invite == null) throw new BizException("邀请码不存在");
        if (invite.getStatus() != 0) throw new BizException("邀请码已被使用");
        if (invite.getExpireTime().isBefore(LocalDateTime.now())) {
            invite.setStatus(2);
            inviteMapper.updateById(invite);
            throw new BizException("邀请码已过期");
        }
        if (invite.getInviterId().equals(uid)) throw new BizException("不能绑定自己");

        User inviter = userMapper.selectById(invite.getInviterId());
        if (inviter == null) throw new BizException("邀请人不存在");
        if (inviter.getCoupleId() != null) throw new BizException("邀请人已经绑定其他伴侣");

        // 创建情侣关系
        Couple couple = new Couple();
        couple.setUserIdA(inviter.getId());
        couple.setUserIdB(self.getId());
        couple.setBindTime(LocalDateTime.now());
        couple.setStatus(0);
        coupleMapper.insert(couple);

        // 双方绑定 couple_id
        inviter.setCoupleId(couple.getId());
        self.setCoupleId(couple.getId());
        userMapper.updateById(inviter);
        userMapper.updateById(self);

        // 邀请码标记已使用
        invite.setStatus(1);
        inviteMapper.updateById(invite);

        // 初始化默认菜单分类（按截图）
        initDefaultCategories(couple.getId());

        // 推送绑定成功通知（异步执行，失败不回滚）
        try {
            subscribeService.sendBindNotify(inviter.getOpenid(), self.getNickname());
            subscribeService.sendBindNotify(self.getOpenid(),    inviter.getNickname());
        } catch (Exception e) {
            log.warn("绑定通知发送失败: {}", e.getMessage());
        }
        return couple;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind() {
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self.getCoupleId() == null) throw new BizException("尚未绑定");
        Couple c = coupleMapper.selectById(self.getCoupleId());
        if (c != null) {
            c.setStatus(1);
            coupleMapper.updateById(c);
            User a = userMapper.selectById(c.getUserIdA());
            User b = userMapper.selectById(c.getUserIdB());
            if (a != null) { a.setCoupleId(null); userMapper.updateById(a); }
            if (b != null) { b.setCoupleId(null); userMapper.updateById(b); }
        }
    }

    @Override
    public User partner() {
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null || self.getCoupleId() == null) return null;
        Couple c = coupleMapper.selectById(self.getCoupleId());
        if (c == null) return null;
        Long otherId = c.getUserIdA().equals(uid) ? c.getUserIdB() : c.getUserIdA();
        return userMapper.selectById(otherId);
    }

    /** 按截图初始化：菜品/水果/零食/饮品/按摩/鲜花/分组管理 */
    private void initDefaultCategories(Long coupleId) {
        String[][] defaults = {
                {"私房菜", "❤️"},
                {"菜品", "🍎"},
                {"水果", "🍎"},
                {"零食", "🍪"},
                {"饮品", "🥤"},
                {"按摩", "🐱"},
                {"鲜花", "🌹"},
                {"分组管理", "⚙️"}
        };
        int sort = 0;
        for (String[] d : defaults) {
            MenuCategory mc = new MenuCategory();
            mc.setCoupleId(coupleId);
            mc.setName(d[0]);
            mc.setIcon(d[1]);
            mc.setSort(sort++);
            categoryMapper.insert(mc);
        }
    }
}
