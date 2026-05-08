package com.babymenu.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.babymenu.common.BizException;
import com.babymenu.entity.CoupleBucketList;
import com.babymenu.entity.Couple;
import com.babymenu.entity.PointsTransaction;
import com.babymenu.entity.User;
import com.babymenu.mapper.BucketListMapper;
import com.babymenu.mapper.CoupleMapper;
import com.babymenu.mapper.PointsTransactionMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.BucketListService;
import com.babymenu.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BucketListServiceImpl implements BucketListService {

    private final BucketListMapper bucketListMapper;
    private final UserMapper userMapper;
    private final CoupleMapper coupleMapper;
    private final PointsTransactionMapper transactionMapper;

    private User getValidUser() {
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null || self.getCoupleId() == null) {
            throw new BizException("请先绑定伴侣");
        }
        return self;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addWish(CoupleBucketList bucketList) {
        User self = getValidUser();
        bucketList.setCoupleId(self.getCoupleId());
        bucketList.setCreatorId(self.getId());
        bucketList.setStatus(0);
        bucketList.setOwnerChecked(false);
        bucketList.setPetChecked(false);
        bucketList.setCreateTime(LocalDateTime.now());
        bucketListMapper.insert(bucketList);
    }

    @Override
    public Page<CoupleBucketList> getList(Integer current, Integer size, Integer status, String category, Integer year) {
        User self = getValidUser();
        Page<CoupleBucketList> page = new Page<>(current, size);
        return bucketListMapper.selectPage(page, Wrappers.<CoupleBucketList>lambdaQuery()
                .eq(CoupleBucketList::getCoupleId, self.getCoupleId())
                .eq(status != null, CoupleBucketList::getStatus, status)
                .eq(category != null && !category.isBlank(), CoupleBucketList::getCategory, category)
                // Filter by year if provided. We check if createTime or targetDate is within the year? Wait, "年度" usually means createTime or targetDate.
                // Let's use targetDate if year is provided, or createTime. If targetDate is string like "2026-05-03", we can use LIKE "2026%".
                .like(year != null, CoupleBucketList::getTargetDate, year != null ? year.toString() + "-" : null)
                .orderByDesc(CoupleBucketList::getCreateTime));
    }

    @Override
    public CoupleBucketList getDetail(Long id) {
        User self = getValidUser();
        CoupleBucketList item = bucketListMapper.selectById(id);
        if (item == null || !item.getCoupleId().equals(self.getCoupleId())) {
            throw new BizException("记录不存在");
        }
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkWish(Long id) {
        User self = getValidUser();
        CoupleBucketList wish = bucketListMapper.selectById(id);
        if (wish == null || !wish.getCoupleId().equals(self.getCoupleId())) {
            throw new BizException("记录不存在");
        }
        if (wish.getStatus() != null && wish.getStatus() == 1) {
            throw new BizException("该愿望已完成");
        }

        String role = self.getRoleInCouple();
        if ("owner".equals(role)) {
            wish.setOwnerChecked(true);
            wish.setOwnerCheckTime(LocalDateTime.now());
        } else if ("pet".equals(role)) {
            wish.setPetChecked(true);
            wish.setPetCheckTime(LocalDateTime.now());
        } else {
            throw new BizException("角色信息异常");
        }

        // 检查是否双方都打卡
        if (Boolean.TRUE.equals(wish.getOwnerChecked()) && Boolean.TRUE.equals(wish.getPetChecked())) {
            wish.setStatus(1);
            wish.setCompleteTime(LocalDateTime.now());
            // 发放积分奖励 (双方各奖励10积分)
            rewardPoints(wish.getCoupleId(), 10, "共同完成愿望: " + wish.getTitle());
        }

        bucketListMapper.updateById(wish);
    }

    private void rewardPoints(Long coupleId, int amount, String note) {
        Couple couple = coupleMapper.selectById(coupleId);
        if (couple != null) {
            rewardUser(couple.getUserIdA(), coupleId, amount, note);
            rewardUser(couple.getUserIdB(), coupleId, amount, note);
        }
    }

    private void rewardUser(Long userId, Long coupleId, int amount, String note) {
        if (userId == null) return;
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPoints((user.getPoints() != null ? user.getPoints() : 0) + amount);
            userMapper.updateById(user);

            PointsTransaction tx = new PointsTransaction();
            tx.setUserId(userId);
            tx.setCoupleId(coupleId);
            tx.setType("reward_received");
            tx.setAmount(amount);
            tx.setNote(note);
            tx.setCreateTime(LocalDateTime.now());
            transactionMapper.insert(tx);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemorialNote(Long id, String note) {
        User self = getValidUser();
        CoupleBucketList wish = bucketListMapper.selectById(id);
        if (wish == null || !wish.getCoupleId().equals(self.getCoupleId())) {
            throw new BizException("记录不存在");
        }
        
        String role = self.getRoleInCouple();
        if ("owner".equals(role)) {
            wish.setMemorialNoteOwner(note);
        } else if ("pet".equals(role)) {
            wish.setMemorialNotePet(note);
        }
        bucketListMapper.updateById(wish);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWish(Long id) {
        User self = getValidUser();
        CoupleBucketList wish = bucketListMapper.selectById(id);
        if (wish == null || !wish.getCoupleId().equals(self.getCoupleId())) {
            throw new BizException("记录不存在");
        }
        bucketListMapper.deleteById(id);
    }
}
