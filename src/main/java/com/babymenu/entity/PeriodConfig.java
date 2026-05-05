package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("period_config")
public class PeriodConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer avgCycleDays;
    private Integer avgPeriodDays;
    private String mode;
    private Integer reminderBeforeDays;
    private Boolean reminderEnabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
