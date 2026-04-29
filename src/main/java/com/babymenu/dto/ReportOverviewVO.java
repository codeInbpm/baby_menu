package com.babymenu.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReportOverviewVO {
    // 基础概览
    private Integer coupleDays;
    private Integer petRequestCount;
    private Integer ownerFinishCount;
    private Integer petCostPoints;
    private Integer ownerRewardPoints;
    private Integer balanceIndex; // 互宠平衡指数 (0-100)
    
    // 宠爱热度日历 (日期 -> 次数)
    private List<DailyCount> heatCalendar;
    
    // 积分趋势 (日期 -> pet消耗, owner收到)
    private List<DailyPoints> pointsTrend;
    
    // 服务类型偏好 Top 5
    private List<ServicePref> topServices;
    
    // 满意度分析
    private Double avgScore;
    private String bestService;
    
    // 高光时刻
    private String highlightMoment;
    
    // 情感总结文案
    private String emotionalSummary;

    @Data
    public static class DailyCount {
        private String date;
        private Integer count;
    }

    @Data
    public static class DailyPoints {
        private String date;
        private Integer cost;
        private Integer earn;
    }

    @Data
    public static class ServicePref {
        private String name;
        private Integer count;
    }
}
