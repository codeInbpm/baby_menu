package com.babymenu.dto;

import lombok.Data;
import java.util.List;

@Data
public class PeriodCareVO {
    private String phaseName;
    private List<String> careTips;
    private List<String> foodsToEat;
    private List<String> foodsToAvoid;
    private String emotionAdvice;
    private String actionAdvice;
}
