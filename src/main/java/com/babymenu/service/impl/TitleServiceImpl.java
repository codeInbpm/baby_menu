package com.babymenu.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.babymenu.dto.TitleVO;
import com.babymenu.entity.*;
import com.babymenu.mapper.*;
import com.babymenu.service.TitleService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TitleServiceImpl implements TitleService {

    private final TitleConfigMapper titleConfigMapper;
    private final UserTitleMapper userTitleMapper;
    private final UserMapper userMapper;
    private final ServiceRequestMapper serviceRequestMapper;
    private final MenuItemMapper menuItemMapper;
    private final MenuCategoryMapper menuCategoryMapper;

    @Override
    public List<TitleVO> listUserTitles(Long userId) {
        User user = userMapper.selectById(userId);
        String role = user.getRoleInCouple(); // owner or pet
        
        List<TitleConfig> allConfigs = titleConfigMapper.selectList(Wrappers.<TitleConfig>lambdaQuery()
                .eq(TitleConfig::getRoleType, role)
                .orderByAsc(TitleConfig::getLevel, TitleConfig::getId));

        List<UserTitle> unlockedTitles = userTitleMapper.selectList(Wrappers.<UserTitle>lambdaQuery()
                .eq(UserTitle::getUserId, userId));
        
        Map<String, UserTitle> unlockedMap = unlockedTitles.stream()
                .collect(Collectors.toMap(UserTitle::getTitleCode, ut -> ut));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return allConfigs.stream().map(config -> {
            TitleVO vo = new TitleVO();
            BeanUtil.copyProperties(config, vo);
            UserTitle ut = unlockedMap.get(config.getTitleCode());
            vo.setUnlocked(ut != null);
            if (ut != null) {
                vo.setIsCurrent(ut.getIsCurrent());
                vo.setUnlockTime(ut.getUnlockTime().format(dtf));
            } else {
                vo.setIsCurrent(false);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void wearTitle(Long userId, String titleCode) {
        // 校验是否解锁
        UserTitle ut = userTitleMapper.selectOne(Wrappers.<UserTitle>lambdaQuery()
                .eq(UserTitle::getUserId, userId)
                .eq(UserTitle::getTitleCode, titleCode));
        if (ut == null) {
            throw new RuntimeException("该称号尚未解锁哦");
        }

        // 取消当前佩戴
        userTitleMapper.update(null, Wrappers.<UserTitle>lambdaUpdate()
                .eq(UserTitle::getUserId, userId)
                .set(UserTitle::getIsCurrent, false));

        // 设置新的佩戴
        ut.setIsCurrent(true);
        userTitleMapper.updateById(ut);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndUnlock(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) return;
        
        String role = user.getRoleInCouple();
        List<TitleConfig> configs = titleConfigMapper.selectList(Wrappers.<TitleConfig>lambdaQuery()
                .eq(TitleConfig::getRoleType, role));
        
        List<UserTitle> alreadyUnlocked = userTitleMapper.selectList(Wrappers.<UserTitle>lambdaQuery()
                .eq(UserTitle::getUserId, userId));
        Set<String> unlockedCodes = alreadyUnlocked.stream()
                .map(UserTitle::getTitleCode).collect(Collectors.toSet());

        // 获取统计数据
        Map<String, Object> stats = getStats(user);

        for (TitleConfig config : configs) {
            if (unlockedCodes.contains(config.getTitleCode())) continue;

            if (isConditionMet(config.getUnlockCondition(), stats)) {
                UserTitle newUt = new UserTitle();
                newUt.setUserId(userId);
                newUt.setCoupleId(user.getCoupleId());
                newUt.setTitleCode(config.getTitleCode());
                newUt.setIsCurrent(false);
                newUt.setUnlockTime(LocalDateTime.now());
                userTitleMapper.insert(newUt);
                log.info("用户 {} 解锁了称号: {}", userId, config.getTitleName());
            }
        }
    }

    @Override
    public TitleVO getCurrentTitle(Long userId) {
        UserTitle ut = userTitleMapper.selectOne(Wrappers.<UserTitle>lambdaQuery()
                .eq(UserTitle::getUserId, userId)
                .eq(UserTitle::getIsCurrent, true)
                .last("LIMIT 1"));
        if (ut == null) return null;
        
        TitleConfig config = titleConfigMapper.selectOne(Wrappers.<TitleConfig>lambdaQuery()
                .eq(TitleConfig::getTitleCode, ut.getTitleCode()));
        
        TitleVO vo = new TitleVO();
        BeanUtil.copyProperties(config, vo);
        vo.setUnlocked(true);
        vo.setIsCurrent(true);
        return vo;
    }

    private Map<String, Object> getStats(User user) {
        Map<String, Object> stats = new HashMap<>();
        Long userId = user.getId();
        String role = user.getRoleInCouple();

        // 基础积分
        stats.put("points", user.getPoints());
        stats.put("reward_points", user.getRewardPoints());

        // 服务记录统计
        List<ServiceRequest> services;
        if ("owner".equals(role)) {
            services = serviceRequestMapper.selectList(Wrappers.<ServiceRequest>lambdaQuery()
                    .eq(ServiceRequest::getToUserId, userId)
                    .eq(ServiceRequest::getStatus, 2)); // 已完成
        } else {
            services = serviceRequestMapper.selectList(Wrappers.<ServiceRequest>lambdaQuery()
                    .eq(ServiceRequest::getFromUserId, userId)
                    .eq(ServiceRequest::getStatus, 2));
        }

        stats.put("total_count", services.size());
        
        // 计算平均分 (排除已使用免责金牌的低分服务)
        double avgScore = services.stream()
                .filter(s -> s.getScore() != null)
                .filter(s -> {
                    // 如果分数 <= 2 且使用了免责金牌，则不计入平均分统计，以保护管家称号
                    if (s.getScore() <= 2 && Boolean.TRUE.equals(s.getIsExemptionUsed() == null ? false : s.getIsExemptionUsed() == 1)) {
                        return false;
                    }
                    return true;
                })
                .mapToInt(ServiceRequest::getScore)
                .average().orElse(0.0);
        stats.put("avg_score", avgScore);

        // 按分类统计
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (ServiceRequest s : services) {
            // 解析 itemIds 获取分类
            List<Long> itemIds = JSONUtil.toList(s.getItemIds(), Long.class);
            if (CollUtil.isNotEmpty(itemIds)) {
                List<MenuItem> items = menuItemMapper.selectBatchIds(itemIds);
                for (MenuItem item : items) {
                    MenuCategory cat = menuCategoryMapper.selectById(item.getCategoryId());
                    if (cat != null) {
                        categoryCounts.put(cat.getName(), categoryCounts.getOrDefault(cat.getName(), 0) + 1);
                    }
                }
            }
        }
        stats.put("category_counts", categoryCounts);

        // 已解锁称号统计
        List<UserTitle> unlocked = userTitleMapper.selectList(Wrappers.<UserTitle>lambdaQuery()
                .eq(UserTitle::getUserId, userId));
        List<String> codes = unlocked.stream().map(UserTitle::getTitleCode).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(codes)) {
            List<TitleConfig> configs = titleConfigMapper.selectList(Wrappers.<TitleConfig>lambdaQuery()
                    .in(TitleConfig::getTitleCode, codes));
            stats.put("rare_count", configs.stream().filter(c -> "rare".equals(c.getLevel())).count());
            stats.put("legend_count", configs.stream().filter(c -> "legend".equals(c.getLevel())).count());
        } else {
            stats.put("rare_count", 0L);
            stats.put("legend_count", 0L);
        }

        return stats;
    }

    private boolean isConditionMet(String conditionJson, Map<String, Object> stats) {
        if (StrUtil.isBlank(conditionJson)) return true;
        JSONObject json = JSONUtil.parseObj(conditionJson);

        // 检查分类次数
        if (json.containsKey("category")) {
            String cat = json.getStr("category");
            int required = json.getInt("count");
            Map<String, Integer> counts = (Map<String, Integer>) stats.get("category_counts");
            // 模糊匹配分类名
            int actual = 0;
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getKey().contains(cat)) {
                    actual += entry.getValue();
                }
            }
            if (actual < required) return false;
        }

        // 检查总次数
        if (json.containsKey("total_service") || json.containsKey("received_service")) {
            int required = json.getInt("total_service", json.getInt("received_service", 0));
            if ((int) stats.get("total_count") < required) return false;
        }

        // 检查积分
        if (json.containsKey("min_reward_points") || json.containsKey("received_points")) {
            int required = json.getInt("min_reward_points", json.getInt("received_points", 0));
            int actual = (int) stats.getOrDefault("reward_points", 0);
            if (actual < required) return false;
        }

        // 检查平均分
        if (json.containsKey("min_avg_score")) {
            double required = json.getDouble("min_avg_score");
            double actual = (double) stats.get("avg_score");
            if (actual < required) return false;
        }

        // 检查稀有称号数
        if (json.containsKey("rare_title_count")) {
            int required = json.getInt("rare_title_count");
            if ((long) stats.get("rare_count") < required) return false;
        }

        return true;
    }
}
