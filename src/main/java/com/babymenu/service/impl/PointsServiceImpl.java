package com.babymenu.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.babymenu.common.BizException;
import com.babymenu.dto.AllocateReqDTO;
import com.babymenu.dto.PointsInfoVO;
import com.babymenu.entity.Couple;
import com.babymenu.entity.PointsTransaction;
import com.babymenu.entity.User;
import com.babymenu.mapper.CoupleMapper;
import com.babymenu.mapper.PointsTransactionMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.PointsService;
import com.babymenu.util.UserContext;
import com.babymenu.wechat.WechatSubscribeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    private final UserMapper userMapper;
    private final CoupleMapper coupleMapper;
    private final PointsTransactionMapper transactionMapper;
    private final WechatSubscribeService subscribeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lazyResetPointsIfNeeded(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;
        String role = user.getRoleInCouple();
        if (role == null) return;

        if ("pet".equals(role)) {
            // Pet 低保重置逻辑：少于 50 补 50，多于 50 保留
            LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            Long count = transactionMapper.selectCount(Wrappers.<PointsTransaction>lambdaQuery()
                    .eq(PointsTransaction::getUserId, userId)
                    .eq(PointsTransaction::getType, "daily_reset")
                    .between(PointsTransaction::getCreateTime, todayStart, todayEnd));
            if (count == null || count == 0) {
                int limit = user.getDailyPointsLimit() != null ? user.getDailyPointsLimit() : 50;
                int current = user.getPoints() != null ? user.getPoints() : 0;
                if (current < limit) {
                    int addAmount = limit - current;
                    user.setPoints(limit);
                    userMapper.updateById(user);
                    PointsTransaction tx = new PointsTransaction();
                    tx.setUserId(userId);
                    tx.setCoupleId(user.getCoupleId());
                    tx.setType("daily_reset");
                    tx.setAmount(addAmount);
                    tx.setCreateTime(LocalDateTime.now());
                    transactionMapper.insert(tx);
                } else {
                    // Record trigger even if no points added to avoid repeat checking
                    PointsTransaction tx = new PointsTransaction();
                    tx.setUserId(userId);
                    tx.setCoupleId(user.getCoupleId());
                    tx.setType("daily_reset");
                    tx.setAmount(0);
                    tx.setCreateTime(LocalDateTime.now());
                    transactionMapper.insert(tx);
                }
            }
        } else if ("owner".equals(role)) {
            // Owner 月初重置逻辑：每月 1 号重置为 0
            LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusNanos(1);
            Long count = transactionMapper.selectCount(Wrappers.<PointsTransaction>lambdaQuery()
                    .eq(PointsTransaction::getUserId, userId)
                    .eq(PointsTransaction::getType, "monthly_reset")
                    .between(PointsTransaction::getCreateTime, monthStart, monthEnd));
            if (count == null || count == 0) {
                int current = user.getPoints() != null ? user.getPoints() : 0;
                user.setPoints(0);
                userMapper.updateById(user);
                PointsTransaction tx = new PointsTransaction();
                tx.setUserId(userId);
                tx.setCoupleId(user.getCoupleId());
                tx.setType("monthly_reset");
                tx.setAmount(-current); // Record how much was wiped
                tx.setCreateTime(LocalDateTime.now());
                transactionMapper.insert(tx);
            }
        }
    }

    @Override
    public PointsInfoVO getInfo() {
        Long uid = UserContext.get();
        lazyResetPointsIfNeeded(uid);

        User self = userMapper.selectById(uid);
        if (self == null) throw new BizException("用户未登录");

        PointsInfoVO vo = new PointsInfoVO();
        
        User partner = null;
        if (self.getCoupleId() != null) {
            Couple couple = coupleMapper.selectById(self.getCoupleId());
            Long partnerId = couple.getUserIdA().equals(uid) ? couple.getUserIdB() : couple.getUserIdA();
            partner = userMapper.selectById(partnerId);
        }
        vo.setPartnerName(partner != null ? partner.getNickname() : "宝贝");

        if ("owner".equals(self.getRoleInCouple())) {
            vo.setCurrentPoints(self.getPoints() != null ? self.getPoints() : 0);
            return vo;
        }

        vo.setCurrentPoints(self.getPoints() != null ? self.getPoints() : 0);
        vo.setDailyLimit(self.getDailyPointsLimit() != null ? self.getDailyPointsLimit() : 50);
        vo.setRemainingToday(vo.getCurrentPoints());
        vo.setCanRequestCount(vo.getCurrentPoints() / 5);

        // 计算今日花费
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        List<PointsTransaction> todayDeducts = transactionMapper.selectList(Wrappers.<PointsTransaction>lambdaQuery()
                .eq(PointsTransaction::getUserId, uid)
                .eq(PointsTransaction::getType, "request_deduct")
                .between(PointsTransaction::getCreateTime, todayStart, todayEnd));
        int used = todayDeducts.stream().mapToInt(PointsTransaction::getAmount).sum();
        vo.setTodayUsed(used);

        return vo;
    }

    @Override
    public List<PointsTransaction> getTransactions() {
        Long uid = UserContext.get();
        return transactionMapper.selectList(Wrappers.<PointsTransaction>lambdaQuery()
                .eq(PointsTransaction::getUserId, uid)
                .orderByDesc(PointsTransaction::getCreateTime)
                .last("LIMIT 20"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void allocate(AllocateReqDTO req) {
        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new BizException("分配积分数值错误");
        }
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null || self.getCoupleId() == null) {
            throw new BizException("请先绑定伴侣");
        }
        
        // 双向真实扣除验证
        int selfPoints = self.getPoints() != null ? self.getPoints() : 0;
        if (selfPoints < req.getAmount()) {
            throw new BizException("您的余额不足，无法分配 " + req.getAmount() + " 积分");
        }

        Couple couple = coupleMapper.selectById(self.getCoupleId());
        Long partnerId = couple.getUserIdA().equals(uid) ? couple.getUserIdB() : couple.getUserIdA();
        
        lazyResetPointsIfNeeded(partnerId);

        User partner = userMapper.selectById(partnerId);
        
        // 扣减自己
        self.setPoints(selfPoints - req.getAmount());
        userMapper.updateById(self);
        
        PointsTransaction txOut = new PointsTransaction();
        txOut.setUserId(uid);
        txOut.setCoupleId(couple.getId());
        txOut.setType("allocate_out");
        txOut.setAmount(-req.getAmount());
        txOut.setNote("分配给" + partner.getNickname());
        txOut.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(txOut);

        // 增加伴侣
        int partnerPoints = partner.getPoints() != null ? partner.getPoints() : 0;
        partner.setPoints(partnerPoints + req.getAmount());
        userMapper.updateById(partner);

        PointsTransaction txIn = new PointsTransaction();
        txIn.setUserId(partnerId);
        txIn.setCoupleId(couple.getId());
        txIn.setType("allocate");
        txIn.setAmount(req.getAmount());
        txIn.setNote(StrUtil.isBlank(req.getNote()) ? "今天也要好好宠你呀～" : req.getNote());
        txIn.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(txIn);

        try {
            subscribeService.sendAllocateNotify(partner.getOpenid(), self.getNickname(), req.getAmount());
        } catch (Exception e) {
            log.warn("发送分配积分订阅消息失败: {}", e.getMessage());
        }
    }
}
