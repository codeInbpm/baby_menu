package com.babymenu.service.impl;

import com.babymenu.entity.MallAvatarFrame;
import com.babymenu.entity.User;
import com.babymenu.entity.UserAvatarItem;
import com.babymenu.entity.UserSettings;
import com.babymenu.entity.PointsTransaction;
import com.babymenu.mapper.MallAvatarFrameMapper;
import com.babymenu.mapper.UserAvatarItemMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.mapper.UserSettingsMapper;
import com.babymenu.mapper.PointsTransactionMapper;
import com.babymenu.service.AvatarFrameService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvatarFrameServiceImpl implements AvatarFrameService {

    private final MallAvatarFrameMapper frameMapper;
    private final UserAvatarItemMapper itemMapper;
    private final UserSettingsMapper settingsMapper;
    private final UserMapper userMapper;
    private final PointsTransactionMapper transactionMapper;

    @Override
    public List<Map<String, Object>> getActiveFrames() {
        return frameMapper.selectList(new QueryWrapper<MallAvatarFrame>().eq("is_active", 1))
                .stream().map(f -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("code", f.getCode());
                    map.put("name", f.getName());
                    map.put("styleDesc", f.getStyleDesc());
                    map.put("price", f.getPrice());
                    return map;
                }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getMyFrames(Long userId) {
        List<String> ownedCodes = itemMapper.selectList(new QueryWrapper<UserAvatarItem>().eq("user_id", userId))
                .stream().map(UserAvatarItem::getFrameCode).collect(Collectors.toList());
        
        UserSettings settings = settingsMapper.selectOne(new QueryWrapper<UserSettings>().eq("user_id", userId));
        String currentCode = settings != null ? settings.getCurrentAvatarFrameCode() : null;

        Map<String, Object> res = new HashMap<>();
        res.put("ownedCodes", ownedCodes);
        res.put("currentCode", currentCode);
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void buyFrame(Long userId, String frameCode) {
        MallAvatarFrame frame = frameMapper.selectOne(new QueryWrapper<MallAvatarFrame>().eq("code", frameCode));
        if (frame == null || frame.getIsActive() == 0) {
            throw new RuntimeException("该头像框不存在或已下架");
        }

        Long count = itemMapper.selectCount(new QueryWrapper<UserAvatarItem>().eq("user_id", userId).eq("frame_code", frameCode));
        if (count > 0) {
            throw new RuntimeException("已经拥有该头像框啦");
        }

        User user = userMapper.selectById(userId);
        if (user.getPoints() < frame.getPrice()) {
            throw new RuntimeException("积分不足，无法兑换");
        }

        // 扣减积分
        user.setPoints(user.getPoints() - frame.getPrice());
        userMapper.updateById(user);

        // 记录流水
        PointsTransaction tx = new PointsTransaction();
        tx.setUserId(user.getId());
        tx.setCoupleId(user.getCoupleId());
        tx.setType("avatar_frame");
        tx.setAmount(-frame.getPrice());
        tx.setNote("兑换头像框：" + frame.getName());
        tx.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tx);

        // 添加物品
        UserAvatarItem item = new UserAvatarItem();
        item.setUserId(userId);
        item.setFrameCode(frameCode);
        itemMapper.insert(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void equipFrame(Long userId, String frameCode) {
        // 如果不是取消穿戴，则校验是否拥有
        if (frameCode != null && !frameCode.isEmpty()) {
            Long count = itemMapper.selectCount(new QueryWrapper<UserAvatarItem>().eq("user_id", userId).eq("frame_code", frameCode));
            if (count == 0) {
                throw new RuntimeException("您还未拥有该头像框");
            }
        } else {
            frameCode = null; // 处理空字符串为取消穿戴
        }

        UserSettings settings = settingsMapper.selectOne(new QueryWrapper<UserSettings>().eq("user_id", userId));
        if (settings == null) {
            settings = new UserSettings();
            settings.setUserId(userId);
            settings.setCurrentAvatarFrameCode(frameCode);
            settingsMapper.insert(settings);
        } else {
            settings.setCurrentAvatarFrameCode(frameCode);
            settingsMapper.updateById(settings);
        }
    }
}
