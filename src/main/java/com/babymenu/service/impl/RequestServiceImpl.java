package com.babymenu.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.babymenu.common.BizException;
import com.babymenu.dto.RequestEvaluateDTO;
import com.babymenu.entity.Couple;
import com.babymenu.entity.MenuItem;
import com.babymenu.entity.ServiceRequest;
import com.babymenu.entity.User;
import com.babymenu.mapper.CoupleMapper;
import com.babymenu.mapper.MenuItemMapper;
import com.babymenu.mapper.ServiceRequestMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.entity.PointsTransaction;
import com.babymenu.mapper.PointsTransactionMapper;
import com.babymenu.service.PointsService;
import com.babymenu.service.RequestService;
import com.babymenu.util.UserContext;
import com.babymenu.wechat.WechatSubscribeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final ServiceRequestMapper requestMapper;
    private final MenuItemMapper itemMapper;
    private final UserMapper userMapper;
    private final CoupleMapper coupleMapper;
    private final WechatSubscribeService subscribeService;
    private final PointsService pointsService;
    private final PointsTransactionMapper transactionMapper;

    @Override
    public ServiceRequest create(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) throw new BizException("请选择至少一项服务");
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null || self.getCoupleId() == null) throw new BizException("请先绑定伴侣");

        Couple couple = coupleMapper.selectById(self.getCoupleId());
        Long toId = couple.getUserIdA().equals(uid) ? couple.getUserIdB() : couple.getUserIdA();
        User to = userMapper.selectById(toId);

        List<MenuItem> items = itemMapper.selectBatchIds(itemIds);
        if (items.isEmpty()) throw new BizException("服务项不存在");
        
        int totalCost = items.stream().mapToInt(item -> item.getPointsCost() != null ? item.getPointsCost() : 5).sum();
        pointsService.lazyResetPointsIfNeeded(uid);
        self = userMapper.selectById(uid);
        if (!"pet".equals(self.getRoleInCouple())) {
            throw new BizException("只有小宝贝(Pet)才能发号施令哦～");
        }
        if (self.getPoints() == null || self.getPoints() < totalCost) {
            throw new BizException("今日可用积分不足 (" + totalCost + "分)");
        }
        
        String content = items.stream().map(MenuItem::getName).collect(Collectors.joining(" + "));

        ServiceRequest req = new ServiceRequest();
        req.setCoupleId(couple.getId());
        req.setFromUserId(uid);
        req.setToUserId(toId);
        req.setItemIds(JSONUtil.toJsonStr(itemIds));
        req.setContent(content);
        req.setStatus(0);
        requestMapper.insert(req);

        // 扣除积分并记账
        self.setPoints(self.getPoints() - totalCost);
        userMapper.updateById(self);
        
        PointsTransaction tx = new PointsTransaction();
        tx.setUserId(uid);
        tx.setCoupleId(couple.getId());
        tx.setType("request_deduct");
        tx.setAmount(totalCost);
        tx.setRelatedRequestId(req.getId());
        tx.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tx);

        try {
            subscribeService.sendRequestNotify(to.getOpenid(), self.getNickname(), content, req.getId());
        } catch (Exception e) {
            log.warn("发送订阅消息失败: {}", e.getMessage());
        }
        return req;
    }

    @Override
    public List<ServiceRequest> list(Integer status) {
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null || self.getCoupleId() == null) return List.of();
        return requestMapper.selectList(
                Wrappers.<ServiceRequest>lambdaQuery()
                        .eq(ServiceRequest::getCoupleId, self.getCoupleId())
                        .eq(status != null, ServiceRequest::getStatus, status)
                        .orderByDesc(ServiceRequest::getCreateTime));
    }

    @Override
    public ServiceRequest detail(Long id) {
        ServiceRequest r = requestMapper.selectById(id);
        if (r == null) throw new BizException("请求不存在");
        return r;
    }

    @Override
    public ServiceRequest accept(Long id) {
        ServiceRequest r = mustOwn(id, true);
        if (r.getStatus() != 0) throw new BizException("当前状态无法接受");
        r.setStatus(1);
        r.setAcceptTime(LocalDateTime.now());
        requestMapper.updateById(r);
        return r;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public ServiceRequest reject(Long id) {
        ServiceRequest r = mustOwn(id, true);
        if (r.getStatus() != 0) throw new BizException("当前状态无法拒绝");
        r.setStatus(3);
        requestMapper.updateById(r);

        // 获取扣除记录以进行返还
        PointsTransaction deductTx = transactionMapper.selectOne(Wrappers.<PointsTransaction>lambdaQuery()
                .eq(PointsTransaction::getRelatedRequestId, id)
                .eq(PointsTransaction::getType, "request_deduct"));
                
        if (deductTx != null && deductTx.getAmount() > 0) {
            User fromUser = userMapper.selectById(r.getFromUserId());
            if (fromUser != null) {
                fromUser.setPoints((fromUser.getPoints() == null ? 0 : fromUser.getPoints()) + deductTx.getAmount());
                userMapper.updateById(fromUser);
                
                // 记账：退款
                PointsTransaction tx = new PointsTransaction();
                tx.setUserId(fromUser.getId());
                tx.setCoupleId(r.getCoupleId());
                tx.setType("request_refund");
                tx.setAmount(deductTx.getAmount());
                tx.setRelatedRequestId(id);
                tx.setNote("对方拒绝了请求，积分退回");
                tx.setCreateTime(LocalDateTime.now());
                transactionMapper.insert(tx);
            }
        }
        return r;
    }

    @Override
    public ServiceRequest finish(Long id) {
        ServiceRequest r = mustOwn(id, true);
        if (r.getStatus() != 1) throw new BizException("请先接受请求");
        r.setStatus(2);
        r.setFinishTime(LocalDateTime.now());
        requestMapper.updateById(r);

        // 将菜品扣除的积分作为收入给到 Owner
        PointsTransaction deductTx = transactionMapper.selectOne(Wrappers.<PointsTransaction>lambdaQuery()
                .eq(PointsTransaction::getRelatedRequestId, id)
                .eq(PointsTransaction::getType, "request_deduct"));
        if (deductTx != null && deductTx.getAmount() > 0) {
            User toUser = userMapper.selectById(r.getToUserId());
            if (toUser != null) {
                toUser.setPoints((toUser.getPoints() == null ? 0 : toUser.getPoints()) + deductTx.getAmount());
                userMapper.updateById(toUser);

                PointsTransaction incomeTx = new PointsTransaction();
                incomeTx.setUserId(toUser.getId());
                incomeTx.setCoupleId(r.getCoupleId());
                incomeTx.setType("request_income");
                incomeTx.setAmount(deductTx.getAmount());
                incomeTx.setRelatedRequestId(id);
                incomeTx.setNote("完成服务，获得积分收益 ✨");
                incomeTx.setCreateTime(LocalDateTime.now());
                transactionMapper.insert(incomeTx);
            }
        }

        return r;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public ServiceRequest evaluate(Long id, RequestEvaluateDTO dto) {
        // Pet evaluates Owner
        Long uid = UserContext.get();
        ServiceRequest r = requestMapper.selectById(id);
        if (r == null) throw new BizException("请求不存在");
        if (!r.getFromUserId().equals(uid)) throw new BizException("只有发起人可以评价");
        if (r.getStatus() != 2) throw new BizException("只能评价已完成的服务");
        if (r.getEvaluatedTime() != null) throw new BizException("该服务已评价过");

        r.setScore(dto.getScore());
        r.setPetRewardPoints(dto.getRewardPoints() != null ? dto.getRewardPoints() : 0);
        r.setPetFeedback(dto.getFeedback());
        r.setEvaluatedTime(LocalDateTime.now());
        requestMapper.updateById(r);

        if (r.getPetRewardPoints() > 0) {
            User toUser = userMapper.selectById(r.getToUserId());
            if (toUser != null) {
                // 专属奖励分：历史累计专属奖励
                toUser.setRewardPoints((toUser.getRewardPoints() != null ? toUser.getRewardPoints() : 0) + r.getPetRewardPoints());
                // 主积分池：可用来发红包
                toUser.setPoints((toUser.getPoints() == null ? 0 : toUser.getPoints()) + r.getPetRewardPoints());
                // 未读动画触发器
                toUser.setHasUnreadReward((toUser.getHasUnreadReward() != null ? toUser.getHasUnreadReward() : 0) + r.getPetRewardPoints());
                userMapper.updateById(toUser);

                PointsTransaction tx = new PointsTransaction();
                tx.setUserId(toUser.getId());
                tx.setCoupleId(r.getCoupleId());
                tx.setType("reward_received");
                tx.setAmount(r.getPetRewardPoints());
                tx.setRelatedRequestId(id);
                tx.setNote("服务完成，收到积分奖励 ❤️");
                tx.setCreateTime(LocalDateTime.now());
                transactionMapper.insert(tx);

                // 记录 Pet 端流水
                PointsTransaction txPet = new PointsTransaction();
                txPet.setUserId(uid);
                txPet.setCoupleId(r.getCoupleId());
                txPet.setType("reward_given");
                txPet.setAmount(-r.getPetRewardPoints()); // 发出奖励对余额无影响，但是作为记录，或者amount设为0。Wait, since we track it just for log, if amount is non-zero, will it affect logic? Our query checks type. We can use amount = r.getPetRewardPoints() with note "赠送积分". Actually let's use 0 to avoid confusing `points` vs `reward_points`. Or just log it with real amount but don't deduct from self.points.
                // It's better to just put the amount but since the point system is isolated, type 'reward_given' won't be calculated in daily spend.
                txPet.setAmount(r.getPetRewardPoints());
                txPet.setRelatedRequestId(id);
                txPet.setNote("奖励给管家积分 ❤️");
                txPet.setCreateTime(LocalDateTime.now());
                transactionMapper.insert(txPet);
            }
        }
        
        // 可选: 通知 Owner
        try {
            User self = userMapper.selectById(uid);
            User to = userMapper.selectById(r.getToUserId());
            // subscribeService.send... 
        } catch (Exception e) {
            log.warn("发送评价通知失败: {}", e.getMessage());
        }

        return r;
    }

    private ServiceRequest mustOwn(Long id, boolean asReceiver) {
        Long uid = UserContext.get();
        ServiceRequest r = requestMapper.selectById(id);
        if (r == null) throw new BizException("请求不存在");
        if (asReceiver && !r.getToUserId().equals(uid)) throw new BizException("无权操作");
        return r;
    }
}
