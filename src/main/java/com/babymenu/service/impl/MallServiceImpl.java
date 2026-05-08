package com.babymenu.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.babymenu.common.BizException;
import com.babymenu.entity.Couple;
import com.babymenu.entity.MallItem;
import com.babymenu.entity.PointsTransaction;
import com.babymenu.entity.User;
import com.babymenu.entity.UserInventory;
import com.babymenu.mapper.CoupleMapper;
import com.babymenu.mapper.MallItemMapper;
import com.babymenu.mapper.PointsTransactionMapper;
import com.babymenu.mapper.UserInventoryMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.MallService;
import com.babymenu.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallServiceImpl implements MallService {

    private final MallItemMapper mallItemMapper;
    private final UserInventoryMapper inventoryMapper;
    private final UserMapper userMapper;
    private final CoupleMapper coupleMapper;
    private final PointsTransactionMapper transactionMapper;

    private User getValidUser() {
        Long uid = UserContext.get();
        if (uid == null) throw new BizException(401, "未登录");
        User user = userMapper.selectById(uid);
        if (user == null || user.getCoupleId() == null) {
            throw new BizException("请先绑定伴侣");
        }
        return user;
    }

    @Override
    public List<MallItem> getAvailableItems() {
        List<MallItem> items = mallItemMapper.selectList(Wrappers.<MallItem>lambdaQuery()
                .eq(MallItem::getStatus, 1)
                .orderByAsc(MallItem::getPrice));
        return items;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void redeemItem(Long itemId) {
        User user = getValidUser();
        MallItem item = mallItemMapper.selectById(itemId);
        if (item == null || item.getStatus() == 0) {
            throw new BizException("商品不存在或已下架");
        }

        // 校验积分
        int currentPoints = user.getPoints() == null ? 0 : user.getPoints();
        if (currentPoints < item.getPrice()) {
            throw new BizException("积分不足");
        }

        // 校验每月限购1次的商品 (如免责金牌 itemType=2)
        if (item.getItemType() == 2) {
            LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
            long count = inventoryMapper.selectCount(Wrappers.<UserInventory>lambdaQuery()
                    .eq(UserInventory::getUserId, user.getId())
                    .eq(UserInventory::getItemType, 2)
                    .ge(UserInventory::getCreateTime, startOfMonth));
            if (count >= 1) {
                throw new BizException("本月已兑换过免责金牌，请下个月再来哦");
            }
        }
        
        // 校验唯一拥有的商品 (如头像框, 皮肤)
        if (item.getItemType() == 3 || item.getItemType() == 5) {
             long count = inventoryMapper.selectCount(Wrappers.<UserInventory>lambdaQuery()
                    .eq(UserInventory::getUserId, user.getId())
                    .eq(UserInventory::getItemId, item.getId()));
             if (count > 0) {
                 throw new BizException("你已经拥有该专属权益，无需重复兑换");
             }
        }

        // 扣除积分
        user.setPoints(currentPoints - item.getPrice());
        userMapper.updateById(user);

        // 记录流水
        PointsTransaction tx = new PointsTransaction();
        tx.setUserId(user.getId());
        tx.setCoupleId(user.getCoupleId());
        tx.setType("mall_redeem");
        tx.setAmount(-item.getPrice());
        tx.setNote("商城兑换: " + item.getName());
        tx.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tx);

        // 发放入背包
        UserInventory inv = new UserInventory();
        inv.setUserId(user.getId());
        inv.setItemId(item.getId());
        inv.setItemType(item.getItemType());
        inv.setItemName(item.getName());
        inv.setStatus(0);
        if (item.getValidityDays() != null) {
            inv.setExpireTime(LocalDateTime.now().plusDays(item.getValidityDays()));
        }
        inv.setCreateTime(LocalDateTime.now());
        inventoryMapper.insert(inv);
        
        // 特殊逻辑：主动触发推送等可以在此处或使用接口触发。为了逻辑清晰，某些商品兑换后即处于"待使用"状态。
    }

    @Override
    public List<UserInventory> getMyInventory() {
        User user = getValidUser();
        // 获取所有未过期或永久有效的权益
        return inventoryMapper.selectList(Wrappers.<UserInventory>lambdaQuery()
                .eq(UserInventory::getUserId, user.getId())
                .and(w -> w.isNull(UserInventory::getExpireTime).or().gt(UserInventory::getExpireTime, LocalDateTime.now()))
                .orderByDesc(UserInventory::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useItem(Long inventoryId, Map<String, Object> extraParams) {
        User user = getValidUser();
        UserInventory inv = inventoryMapper.selectById(inventoryId);
        if (inv == null || !inv.getUserId().equals(user.getId())) {
            throw new BizException("权益不存在");
        }
        if (inv.getStatus() == 1 && (inv.getItemType() == 1 || inv.getItemType() == 2 || inv.getItemType() == 4)) {
            throw new BizException("该权益已被使用");
        }
        if (inv.getExpireTime() != null && inv.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException("该权益已过期");
        }

        // 标记为已使用 (消耗品)
        if (inv.getItemType() == 1 || inv.getItemType() == 2 || inv.getItemType() == 4) {
            inv.setStatus(1);
            inv.setUseTime(LocalDateTime.now());
            inventoryMapper.updateById(inv);
        }
        
        // 根据类型执行具体逻辑 (例如发送通知，在真实场景中可以接入微信订阅消息或极光推送)
        // 1 - 公主主动服务卡
        if (inv.getItemType() == 1) {
            // TODO: 发送高优先级提醒给公主端
            log.info("向公主端推送: 你的管家刚刚使用了公主主动服务卡～他今天好想被你宠一下哦！");
        }
        // 2 - 免责金牌
        else if (inv.getItemType() == 2) {
            // 在具体的请求(request)服务完成/失败时，检查是否有免责金牌被使用。此处可绑定 extraParams 中的 requestId
            if (extraParams != null && extraParams.containsKey("requestId")) {
                inv.setExtraData("requestId:" + extraParams.get("requestId"));
                inventoryMapper.updateById(inv);
            }
        }
        // 4 - 公主告白券
        else if (inv.getItemType() == 4) {
            log.info("向公主端推送: 你的管家使用了公主告白券，他想听你夸夸他～");
        }
        // 5 - 皮肤切换
        else if (inv.getItemType() == 5 || inv.getItemType() == 3) {
            // 皮肤或头像框可以重复切换使用，不标记为消耗完，只是更新 extraData 记录当前选择的款式
            if (extraParams != null && extraParams.containsKey("style")) {
                inv.setExtraData(String.valueOf(extraParams.get("style")));
                inventoryMapper.updateById(inv);
            }
        }
    }
}
