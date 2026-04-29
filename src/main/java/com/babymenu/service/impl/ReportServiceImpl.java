package com.babymenu.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.babymenu.common.BizException;
import com.babymenu.util.UserContext;
import com.babymenu.dto.ReportOverviewVO;
import com.babymenu.entity.Couple;
import com.babymenu.entity.CoupleMemorial;
import com.babymenu.entity.MenuItem;
import com.babymenu.entity.PointsTransaction;
import com.babymenu.entity.ServiceRequest;
import com.babymenu.entity.User;
import com.babymenu.mapper.CoupleMapper;
import com.babymenu.mapper.CoupleMemorialMapper;
import com.babymenu.mapper.MenuItemMapper;
import com.babymenu.mapper.PointsTransactionMapper;
import com.babymenu.mapper.ServiceRequestMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.ReportService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final UserMapper userMapper;
    private final CoupleMapper coupleMapper;
    private final ServiceRequestMapper requestMapper;
    private final PointsTransactionMapper transactionMapper;
    private final MenuItemMapper itemMapper;
    private final CoupleMemorialMapper memorialMapper;

    @Override
    public ReportOverviewVO getOverview(String type) {
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null || self.getCoupleId() == null) {
            throw new BizException("需先绑定情侣关系才能查看宠爱报表哦");
        }

        Couple couple = coupleMapper.selectById(self.getCoupleId());
        if (couple == null) throw new BizException("情侣关系异常");

        // 计算时间区间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime;
        switch (type) {
            case "week":
                startTime = now.minusWeeks(1).with(LocalTime.MIN);
                break;
            case "quarter":
                startTime = now.minusMonths(3).with(LocalTime.MIN);
                break;
            case "year":
                startTime = now.minusYears(1).with(LocalTime.MIN);
                break;
            case "month":
            default:
                startTime = now.minusMonths(1).with(LocalTime.MIN);
                break;
        }

        ReportOverviewVO vo = new ReportOverviewVO();
        
        // 计算天数，优先使用主纪念日
        CoupleMemorial mainMemorial = memorialMapper.selectOne(Wrappers.<CoupleMemorial>lambdaQuery()
                .eq(CoupleMemorial::getCoupleId, couple.getId())
                .eq(CoupleMemorial::getIsMain, true)
                .last("LIMIT 1"));
        if (mainMemorial != null && mainMemorial.getMemorialDate() != null) {
            vo.setCoupleDays((int) ChronoUnit.DAYS.between(mainMemorial.getMemorialDate().atStartOfDay(), now) + 1);
        } else {
            vo.setCoupleDays((int) ChronoUnit.DAYS.between(couple.getBindTime(), now) + 1);
        }

        // 获取该周期的所有请求
        List<ServiceRequest> requests = requestMapper.selectList(Wrappers.<ServiceRequest>lambdaQuery()
                .eq(ServiceRequest::getCoupleId, couple.getId())
                .ge(ServiceRequest::getCreateTime, startTime)
                .orderByAsc(ServiceRequest::getCreateTime));

        // 获取该周期的所有积分流水
        List<PointsTransaction> txs = transactionMapper.selectList(Wrappers.<PointsTransaction>lambdaQuery()
                .eq(PointsTransaction::getCoupleId, couple.getId())
                .ge(PointsTransaction::getCreateTime, startTime)
                .orderByAsc(PointsTransaction::getCreateTime));

        // 识别 Pet 和 Owner ID
        Long petId = null;
        Long ownerId = null;
        if (couple.getPetId() != null && couple.getOwnerId() != null) {
            petId = couple.getPetId();
            ownerId = couple.getOwnerId();
        } else {
            // 兜底逻辑：通过用户的 role_in_couple 推断
            if ("pet".equals(self.getRoleInCouple())) {
                petId = uid;
                ownerId = couple.getUserIdA().equals(uid) ? couple.getUserIdB() : couple.getUserIdA();
            } else {
                ownerId = uid;
                petId = couple.getUserIdA().equals(uid) ? couple.getUserIdB() : couple.getUserIdA();
            }
        }

        // 1. 基础概览
        int petReqCount = 0;
        int ownerFinCount = 0;
        double totalScore = 0;
        int scoreCount = 0;
        ServiceRequest bestReq = null;
        ServiceRequest highlightReq = null;

        Map<String, Integer> heatMap = new HashMap<>();
        Map<Long, Integer> itemCounts = new HashMap<>();

        for (ServiceRequest r : requests) {
            if (r.getFromUserId().equals(petId)) {
                petReqCount++;
            }
            if (r.getStatus() == 2 && r.getToUserId().equals(ownerId)) {
                ownerFinCount++;
            }
            if (r.getScore() != null) {
                totalScore += r.getScore();
                scoreCount++;
                if (bestReq == null || r.getScore() > bestReq.getScore()) {
                    bestReq = r;
                }
            }
            if (r.getPetRewardPoints() != null && r.getPetRewardPoints() > 0) {
                if (highlightReq == null || r.getPetRewardPoints() > highlightReq.getPetRewardPoints()) {
                    highlightReq = r;
                }
            }

            // 热力图
            String dateStr = r.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            heatMap.put(dateStr, heatMap.getOrDefault(dateStr, 0) + 1);

            // 解析 itemIds
            try {
                List<Long> ids = JSONUtil.toList(r.getItemIds(), Long.class);
                for (Long id : ids) {
                    itemCounts.put(id, itemCounts.getOrDefault(id, 0) + 1);
                }
            } catch (Exception ignored) {}
        }

        vo.setPetRequestCount(petReqCount);
        vo.setOwnerFinishCount(ownerFinCount);
        vo.setAvgScore(scoreCount > 0 ? (totalScore / scoreCount) : 0.0);

        // 积分概览 & 折线图
        int petCost = 0;
        int ownerEarn = 0;
        Map<String, ReportOverviewVO.DailyPoints> trendMap = new TreeMap<>(); // 保证日期有序

        for (PointsTransaction tx : txs) {
            String dateStr = tx.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            trendMap.putIfAbsent(dateStr, new ReportOverviewVO.DailyPoints());
            ReportOverviewVO.DailyPoints dp = trendMap.get(dateStr);
            dp.setDate(dateStr);
            if (dp.getCost() == null) dp.setCost(0);
            if (dp.getEarn() == null) dp.setEarn(0);

            if ("request_deduct".equals(tx.getType()) && tx.getUserId().equals(petId)) {
                petCost += Math.abs(tx.getAmount());
                dp.setCost(dp.getCost() + Math.abs(tx.getAmount()));
            }
            if (("reward_received".equals(tx.getType()) || "request_income".equals(tx.getType())) 
                && tx.getUserId().equals(ownerId)) {
                ownerEarn += Math.abs(tx.getAmount());
                dp.setEarn(dp.getEarn() + Math.abs(tx.getAmount()));
            }
        }
        vo.setPetCostPoints(petCost);
        vo.setOwnerRewardPoints(ownerEarn);
        vo.setPointsTrend(new ArrayList<>(trendMap.values()));

        // 平衡指数: (完成次数 / 发起次数) 的百分比，最高100
        if (petReqCount > 0) {
            vo.setBalanceIndex(Math.min(100, (int) ((ownerFinCount * 100.0) / petReqCount)));
        } else {
            vo.setBalanceIndex(100);
        }

        // 热度日历构建
        List<ReportOverviewVO.DailyCount> heatList = new ArrayList<>();
        heatMap.forEach((date, count) -> {
            ReportOverviewVO.DailyCount dc = new ReportOverviewVO.DailyCount();
            dc.setDate(date);
            dc.setCount(count);
            heatList.add(dc);
        });
        heatList.sort(Comparator.comparing(ReportOverviewVO.DailyCount::getDate));
        vo.setHeatCalendar(heatList);

        // 偏好 Top 5
        List<ReportOverviewVO.ServicePref> topList = new ArrayList<>();
        if (!itemCounts.isEmpty()) {
            List<MenuItem> allItems = itemMapper.selectBatchIds(itemCounts.keySet());
            Map<Long, String> itemNameMap = allItems.stream().collect(Collectors.toMap(MenuItem::getId, MenuItem::getName));

            itemCounts.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .forEach(e -> {
                        ReportOverviewVO.ServicePref pref = new ReportOverviewVO.ServicePref();
                        pref.setName(itemNameMap.getOrDefault(e.getKey(), "已删服务"));
                        pref.setCount(e.getValue());
                        topList.add(pref);
                    });
        }
        vo.setTopServices(topList);

        // 最佳与高光
        if (bestReq != null) {
            vo.setBestService("最高评分: " + bestReq.getContent() + " (" + bestReq.getScore() + "星)");
        }
        if (highlightReq != null) {
            vo.setHighlightMoment("单次最大打赏: " + highlightReq.getPetRewardPoints() + "积分 (" + highlightReq.getContent() + ")");
        } else {
            vo.setHighlightMoment("平平淡淡才是真，期待下一次的惊喜打赏～");
        }

        // 情感文案
        if (!topList.isEmpty()) {
            ReportOverviewVO.ServicePref top = topList.get(0);
            vo.setEmotionalSummary("本周期内，宝贝最离不开的服务是「" + top.getName() + "」，总共点了 " + top.getCount() + " 次。你就是 TA 的专属守护者，继续用爱包围 TA 吧 ❤️");
        } else {
            vo.setEmotionalSummary("这段时间似乎比较安静哦，快去发起一些心动服务，给你们的生活加点甜度吧 ❤️");
        }

        return vo;
    }
}
