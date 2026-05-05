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
    public Couple bindByCode(String code, String role) {
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

        // 确定角色 (默认: self 为 owner, inviter 为 pet)
        String selfRole = "owner";
        String inviterRole = "pet";
        if ("pet".equals(role)) {
            selfRole = "pet";
            inviterRole = "owner";
        }

        // 创建情侣关系
        Couple couple = new Couple();
        couple.setUserIdA(inviter.getId());
        couple.setUserIdB(self.getId());
        if ("pet".equals(selfRole)) {
            couple.setPetId(self.getId());
            couple.setOwnerId(inviter.getId());
        } else {
            couple.setPetId(inviter.getId());
            couple.setOwnerId(self.getId());
        }
        couple.setBindTime(LocalDateTime.now());
        couple.setStatus(0);
        coupleMapper.insert(couple);

        // 双方绑定 couple_id 和角色
        inviter.setCoupleId(couple.getId());
        inviter.setRoleInCouple(inviterRole);
        self.setCoupleId(couple.getId());
        self.setRoleInCouple(selfRole);
        userMapper.updateById(inviter);
        userMapper.updateById(self);

        // 邀请码标记已使用
        invite.setStatus(1);
        inviteMapper.updateById(invite);

        // 初始化默认菜单分类（按截图）
        initDefaultCategories(couple.getId());

        // 推送绑定成功通知
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
            if (a != null) { 
                userMapper.update(null, Wrappers.<User>lambdaUpdate()
                        .set(User::getCoupleId, null)
                        .set(User::getRoleInCouple, null)
                        .eq(User::getId, a.getId())); 
            }
            if (b != null) { 
                userMapper.update(null, Wrappers.<User>lambdaUpdate()
                        .set(User::getCoupleId, null)
                        .set(User::getRoleInCouple, null)
                        .eq(User::getId, b.getId())); 
            }
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

    @Override
    public Couple getCoupleInfo() {
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null || self.getCoupleId() == null) return null;
        return coupleMapper.selectById(self.getCoupleId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestSwitchRole() {
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null || self.getCoupleId() == null) throw new BizException("请先绑定伴侣");
        
        Couple couple = coupleMapper.selectById(self.getCoupleId());
        if (couple.getSwitchRolePending() != null && couple.getSwitchRolePending()) {
            throw new BizException("已存在待处理的请求哦");
        }
        
        couple.setSwitchRolePending(true);
        couple.setSwitchRoleApplicant(uid);
        coupleMapper.updateById(couple);
        
        Long partnerId = couple.getUserIdA().equals(uid) ? couple.getUserIdB() : couple.getUserIdA();
        User partner = userMapper.selectById(partnerId);
        
        try {
            subscribeService.sendSwitchRoleRequestNotify(partner.getOpenid(), self.getNickname());
        } catch (Exception e) {
            log.warn("发送互换请求通知失败: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptSwitchRole() {
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null || self.getCoupleId() == null) throw new BizException("请先绑定伴侣");
        
        Couple couple = coupleMapper.selectById(self.getCoupleId());
        if (couple.getSwitchRolePending() == null || !couple.getSwitchRolePending()) {
            throw new BizException("没有待处理的球换请求哦");
        }
        
        if (uid.equals(couple.getSwitchRoleApplicant())) {
            throw new BizException("不能自己同意自己发起的请求哦");
        }
        
        // 执行翻转
        Long applicantId = couple.getSwitchRoleApplicant();
        User applicant = userMapper.selectById(applicantId);
        
        String tempRole = self.getRoleInCouple();
        self.setRoleInCouple(applicant.getRoleInCouple());
        applicant.setRoleInCouple(tempRole);
        
        userMapper.updateById(self);
        userMapper.updateById(applicant);
        
        // 翻转夫妇内 pet 和 owner
        Long tempPetId = couple.getPetId();
        couple.setPetId(couple.getOwnerId());
        couple.setOwnerId(tempPetId);
        
        couple.setSwitchRolePending(false);
        couple.setSwitchRoleApplicant(null);
        coupleMapper.updateById(couple);
        
        try {
            subscribeService.sendSwitchRoleAcceptNotify(applicant.getOpenid(), self.getNickname());
            subscribeService.sendSwitchRoleAcceptNotify(self.getOpenid(), applicant.getNickname());
        } catch (Exception e) {
            log.warn("发送互换成功通知失败: {}", e.getMessage());
        }
    }
}
