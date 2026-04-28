package com.babymenu.dto;

import lombok.Data;

@Data
public class PointsInfoVO {
    private Integer currentPoints;
    private Integer dailyLimit;
    private Integer todayUsed;
    private Integer remainingToday;
    private Integer canRequestCount;
    private String partnerName;
    private Integer todayReceivedCount;
}
