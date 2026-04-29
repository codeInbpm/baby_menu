package com.babymenu.dto;

import com.babymenu.entity.TitleConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TitleVO extends TitleConfig {
    private Boolean unlocked;
    private Boolean isCurrent;
    private String unlockTime;
}
