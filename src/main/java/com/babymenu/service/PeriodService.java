package com.babymenu.service;

import com.babymenu.entity.PeriodConfig;
import com.babymenu.entity.PeriodRecord;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

public interface PeriodService {
    PeriodOverviewVO getOverview(Long userId);
    List<PeriodRecord> getCalendar(Long userId, String yearMonth);
    void saveRecord(PeriodRecord record);
    PeriodConfig getConfig(Long userId);
    void saveConfig(PeriodConfig config);
    PeriodAnalysisVO getAnalysis(Long userId);
    com.babymenu.dto.PeriodCareVO getCareDetail(Long userId, String date);

    @Data
    class PeriodOverviewVO {
        private String status; // 经期中, 安全期, 排卵期, 即将来姨妈
        private Integer dayInCycle;
        private Integer daysUntilNext;
        private String dailyTip;
        private LocalDate predictStartDate;
        private LocalDate predictEndDate;
        private LocalDate ovulationDate;
        private Boolean isPeriod; // 今天是否在经期
        private com.babymenu.dto.PeriodCareVO todayCare;
    }

    @Data
    class PeriodAnalysisVO {
        private List<Integer> cycleTrends; // 最近6次周期长度
        private List<String> cycleDates;   // 对应的开始日期
        private List<MoodStat> moodStats;  // 情绪分布
    }

    @Data
    class MoodStat {
        private String mood;
        private Integer count;
    }
}
