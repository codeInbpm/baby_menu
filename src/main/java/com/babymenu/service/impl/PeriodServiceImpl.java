package com.babymenu.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.babymenu.common.BizException;
import com.babymenu.entity.PeriodConfig;
import com.babymenu.entity.PeriodCycle;
import com.babymenu.entity.PeriodRecord;
import com.babymenu.mapper.PeriodConfigMapper;
import com.babymenu.mapper.PeriodCycleMapper;
import com.babymenu.mapper.PeriodRecordMapper;
import com.babymenu.service.PeriodService;
import com.babymenu.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PeriodServiceImpl implements PeriodService {

    private final PeriodConfigMapper configMapper;
    private final PeriodRecordMapper recordMapper;
    private final PeriodCycleMapper cycleMapper;

    @Override
    public PeriodOverviewVO getOverview(Long userId) {
        PeriodConfig config = getConfig(userId);
        List<PeriodCycle> cycles = cycleMapper.selectList(Wrappers.<PeriodCycle>lambdaQuery()
                .eq(PeriodCycle::getUserId, userId)
                .orderByDesc(PeriodCycle::getStartDate)
                .last("limit 6"));

        PeriodOverviewVO vo = new PeriodOverviewVO();
        LocalDate today = LocalDate.now();

        // 1. 计算加权平均周期天数
        double avgCycle = calculateWeightedAvg(cycles, config.getAvgCycleDays());
        double std = calculateStd(cycles, avgCycle);

        // 2. 获取最近一次开始日期
        LocalDate lastStart = cycles.isEmpty() ? null : cycles.get(0).getStartDate();
        
        // 如果没有记录，返回默认值
        if (lastStart == null) {
            vo.setStatus("欢迎使用");
            vo.setDailyTip("记录第一次经期开始，我将为您守护 ❤️");
            return vo;
        }

        // 3. 预测下一次
        LocalDate nextPredictStart = lastStart.plusDays((long) avgCycle);
        long daysUntilNext = ChronoUnit.DAYS.between(today, nextPredictStart);
        
        vo.setDaysUntilNext((int) daysUntilNext);
        vo.setPredictStartDate(nextPredictStart.minusDays((long) Math.ceil(std)));
        vo.setPredictEndDate(nextPredictStart.plusDays((long) Math.ceil(std)));
        
        // 4. 判断当前状态
        long dayInCycle = ChronoUnit.DAYS.between(lastStart, today) + 1;
        vo.setDayInCycle((int) dayInCycle);

        // 检查今天是否在经期（根据记录）
        PeriodRecord todayRecord = recordMapper.selectOne(Wrappers.<PeriodRecord>lambdaQuery()
                .eq(PeriodRecord::getUserId, userId)
                .eq(PeriodRecord::getRecordDate, today));
        boolean isPeriod = todayRecord != null && todayRecord.getIsPeriod();
        vo.setIsPeriod(isPeriod);

        // 排卵日预测 (下次经期前14天)
        LocalDate ovulationDate = nextPredictStart.minusDays(14);
        vo.setOvulationDate(ovulationDate);

        if (isPeriod) {
            vo.setStatus("经期中");
            vo.setDailyTip("记得多喝温水，注意保暖哦 ☕");
        } else if (today.isAfter(ovulationDate.minusDays(3)) && today.isBefore(ovulationDate.plusDays(3))) {
            vo.setStatus("排卵期");
            vo.setDailyTip("现在是排卵期，注意身体变化 ✨");
        } else if (daysUntilNext <= 3 && daysUntilNext > 0) {
            vo.setStatus("即将到来");
            vo.setDailyTip("大姨妈快要来访了，提前备好用品吧 🧴");
        } else {
            vo.setStatus("安全期");
            vo.setDailyTip("目前是安全期，保持心情愉悦 🌸");
        }

        vo.setTodayCare(generateCareAdvice(userId, vo.getStatus(), today));

        return vo;
    }

    private com.babymenu.dto.PeriodCareVO generateCareAdvice(Long userId, String phaseStatus, LocalDate targetDate) {
        com.babymenu.dto.PeriodCareVO care = new com.babymenu.dto.PeriodCareVO();
        care.setPhaseName(phaseStatus);

        // 获取女方最近一次的有痛经记录的打卡
        PeriodRecord recentRecord = recordMapper.selectOne(Wrappers.<PeriodRecord>lambdaQuery()
                .eq(PeriodRecord::getUserId, userId)
                .isNotNull(PeriodRecord::getPainLevel)
                .orderByDesc(PeriodRecord::getRecordDate)
                .last("limit 1"));
        
        int painLevel = recentRecord != null && recentRecord.getPainLevel() != null ? recentRecord.getPainLevel() : 0;
        
        List<String> tips = new ArrayList<>();
        List<String> eat = new ArrayList<>();
        List<String> avoid = new ArrayList<>();
        
        if ("经期中".equals(phaseStatus)) {
            tips.add("她现在处于生理期，身体可能会比较虚弱，多给她一些拥抱。");
            tips.add("帮她准备一个热水袋或者暖宝宝贴在小腹上。");
            if (painLevel >= 2) {
                tips.add("由于她平时痛经较严重，建议提前备好止痛药，并帮她轻柔按摩后腰。");
            }
            eat.add("红糖姜茶 - 驱寒暖宫");
            eat.add("温热的粥类 - 易消化");
            avoid.add("冰淇淋、冷饮 - 加重痛经");
            avoid.add("辛辣刺激食物 - 容易引起不适");
            care.setEmotionAdvice("她今天可能会有些烦躁或情绪低落，请保持极大的耐心，多顺从她，不要在这个时候讲大道理。");
            care.setActionAdvice("主动承担家务，让她多休息。可以问她：'宝贝，需不需要我帮你揉揉肚子？'");
        } else if ("即将到来".equals(phaseStatus)) {
            tips.add("姨妈快到了，也就是俗称的'经前期综合征'高发期。");
            tips.add("提前检查家里的卫生用品是否充足。");
            eat.add("富含维生素B6的食物（香蕉、燕麦） - 帮助稳定情绪");
            avoid.add("过咸的食物 - 容易引起水肿");
            care.setEmotionAdvice("这个阶段她可能会无名火起或者变得敏感脆弱，记得多包容，不要和她吵架。");
            care.setActionAdvice("准备一些小惊喜或者她爱吃的零食，哄她开心。");
        } else if ("排卵期".equals(phaseStatus)) {
            tips.add("排卵期雌激素水平变化，注意身体保暖。");
            eat.add("富含蛋白质的食物 - 补充营养");
            avoid.add("过于生冷的食物");
            care.setEmotionAdvice("这个阶段心情一般比较平和，偶尔会有波动，正常陪伴即可。");
            care.setActionAdvice("适合安排一次浪漫的约会或者一起出去走走。");
        } else {
            tips.add("目前是安全期/卵泡期，身体状态最佳！");
            tips.add("可以陪她一起做些运动，或者去吃大餐。");
            eat.add("均衡饮食即可");
            avoid.add("无特殊忌口");
            care.setEmotionAdvice("她现在心情应该不错，也是你们沟通感情的好时机。");
            care.setActionAdvice("带她去吃一直想吃的那家店吧！");
        }
        
        care.setCareTips(tips);
        care.setFoodsToEat(eat);
        care.setFoodsToAvoid(avoid);
        return care;
    }

    @Override
    public com.babymenu.dto.PeriodCareVO getCareDetail(Long userId, String date) {
        LocalDate target = LocalDate.parse(date);
        
        PeriodRecord targetRecord = recordMapper.selectOne(Wrappers.<PeriodRecord>lambdaQuery()
                .eq(PeriodRecord::getUserId, userId)
                .eq(PeriodRecord::getRecordDate, target));
        boolean isPeriod = targetRecord != null && Boolean.TRUE.equals(targetRecord.getIsPeriod());
        
        String status;
        if (isPeriod) {
            status = "经期中";
        } else {
            // 简单预测当天的状态
            PeriodOverviewVO overview = getOverview(userId);
            if (overview.getOvulationDate() != null && target.isAfter(overview.getOvulationDate().minusDays(3)) && target.isBefore(overview.getOvulationDate().plusDays(3))) {
                status = "排卵期";
            } else if (overview.getPredictStartDate() != null) {
                 long daysUntilNext = ChronoUnit.DAYS.between(target, overview.getPredictStartDate());
                 if (daysUntilNext <= 3 && daysUntilNext > 0) {
                     status = "即将到来";
                 } else {
                     status = "安全期";
                 }
            } else {
                status = "安全期";
            }
        }
        
        return generateCareAdvice(userId, status, target);
    }

    private double calculateWeightedAvg(List<PeriodCycle> cycles, int defaultValue) {
        if (cycles.isEmpty()) return defaultValue;
        
        List<Integer> lengths = cycles.stream()
                .map(PeriodCycle::getCycleLength)
                .filter(l -> l != null && l >= 20 && l <= 45)
                .collect(Collectors.toList());
        
        if (lengths.isEmpty()) return defaultValue;
        
        double sum = 0;
        double weightSum = 0;
        for (int i = 0; i < lengths.size(); i++) {
            double weight = i + 1; // 越新权重越高
            sum += lengths.get(lengths.size() - 1 - i) * weight;
            weightSum += weight;
        }
        return sum / weightSum;
    }

    private double calculateStd(List<PeriodCycle> cycles, double avg) {
        if (cycles.size() < 2) return 1.5; // 默认波动
        List<Integer> lengths = cycles.stream()
                .map(PeriodCycle::getCycleLength)
                .filter(l -> l != null && l >= 20 && l <= 45)
                .collect(Collectors.toList());
        if (lengths.size() < 2) return 1.5;
        
        double variance = 0;
        for (int l : lengths) {
            variance += Math.pow(l - avg, 2);
        }
        return Math.sqrt(variance / lengths.size());
    }

    @Override
    public List<PeriodRecord> getCalendar(Long userId, String yearMonth) {
        LocalDate start = LocalDate.parse(yearMonth + "-01");
        LocalDate end = start.plusMonths(1).minusDays(1);
        return recordMapper.selectList(Wrappers.<PeriodRecord>lambdaQuery()
                .eq(PeriodRecord::getUserId, userId)
                .between(PeriodRecord::getRecordDate, start, end));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRecord(PeriodRecord record) {
        Long userId = UserContext.get();
        record.setUserId(userId);
        
        // 检查是否是新周期的开始 (从非经期变为经期)
        if (record.getIsPeriod()) {
            PeriodRecord prevDay = recordMapper.selectOne(Wrappers.<PeriodRecord>lambdaQuery()
                    .eq(PeriodRecord::getUserId, userId)
                    .eq(PeriodRecord::getRecordDate, record.getRecordDate().minusDays(1)));
            
            if (prevDay == null || !prevDay.getIsPeriod()) {
                // 是新周期的开始
                handleNewCycle(userId, record.getRecordDate());
            }
        }

        PeriodRecord existing = recordMapper.selectOne(Wrappers.<PeriodRecord>lambdaQuery()
                .eq(PeriodRecord::getUserId, userId)
                .eq(PeriodRecord::getRecordDate, record.getRecordDate()));
        
        if (existing != null) {
            record.setId(existing.getId());
            recordMapper.updateById(record);
        } else {
            recordMapper.insert(record);
        }
    }

    private void handleNewCycle(Long userId, LocalDate startDate) {
        PeriodCycle lastCycle = cycleMapper.selectOne(Wrappers.<PeriodCycle>lambdaQuery()
                .eq(PeriodCycle::getUserId, userId)
                .orderByDesc(PeriodCycle::getStartDate)
                .last("limit 1"));
        
        if (lastCycle != null) {
            // 更新上一个周期的结束日期和长度
            lastCycle.setEndDate(startDate.minusDays(1));
            lastCycle.setCycleLength((int) ChronoUnit.DAYS.between(lastCycle.getStartDate(), startDate));
            cycleMapper.updateById(lastCycle);
        }
        
        // 创建新周期
        PeriodCycle newCycle = new PeriodCycle();
        newCycle.setUserId(userId);
        newCycle.setStartDate(startDate);
        cycleMapper.insert(newCycle);
    }

    @Override
    public PeriodConfig getConfig(Long userId) {
        PeriodConfig config = configMapper.selectOne(Wrappers.<PeriodConfig>lambdaQuery()
                .eq(PeriodConfig::getUserId, userId));
        if (config == null) {
            config = new PeriodConfig();
            config.setUserId(userId);
            config.setAvgCycleDays(28);
            config.setAvgPeriodDays(5);
            config.setMode("normal");
            config.setReminderEnabled(true);
            config.setReminderBeforeDays(3);
            configMapper.insert(config);
        }
        return config;
    }

    @Override
    public void saveConfig(PeriodConfig config) {
        config.setUserId(UserContext.get());
        if (config.getId() != null) {
            configMapper.updateById(config);
        } else {
            configMapper.insert(config);
        }
    }

    @Override
    public PeriodAnalysisVO getAnalysis(Long userId) {
        PeriodAnalysisVO vo = new PeriodAnalysisVO();
        
        // 1. 周期长度趋势
        List<PeriodCycle> cycles = cycleMapper.selectList(Wrappers.<PeriodCycle>lambdaQuery()
                .eq(PeriodCycle::getUserId, userId)
                .isNotNull(PeriodCycle::getCycleLength)
                .orderByDesc(PeriodCycle::getStartDate)
                .last("limit 6"));
        
        List<Integer> trends = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        for (int i = cycles.size() - 1; i >= 0; i--) {
            trends.add(cycles.get(i).getCycleLength());
            dates.add(cycles.get(i).getStartDate().toString());
        }
        vo.setCycleTrends(trends);
        vo.setCycleDates(dates);

        // 2. 情绪分布
        List<PeriodRecord> recentRecords = recordMapper.selectList(Wrappers.<PeriodRecord>lambdaQuery()
                .eq(PeriodRecord::getUserId, userId)
                .isNotNull(PeriodRecord::getMood)
                .ne(PeriodRecord::getMood, "")
                .orderByDesc(PeriodRecord::getRecordDate)
                .last("limit 30"));
        
        java.util.Map<String, Long> moodCounts = recentRecords.stream()
                .collect(Collectors.groupingBy(PeriodRecord::getMood, Collectors.counting()));
        
        List<MoodStat> stats = moodCounts.entrySet().stream()
                .map(e -> {
                    MoodStat s = new MoodStat();
                    s.setMood(e.getKey());
                    s.setCount(e.getValue().intValue());
                    return s;
                })
                .collect(Collectors.toList());
        vo.setMoodStats(stats);
        
        return vo;
    }
}
