package com.babymenu.dto;

import lombok.Data;

@Data
public class RequestEvaluateDTO {
    private Integer score;
    private Integer rewardPoints;
    private String feedback;
}
