package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("period_cycle")
public class PeriodCycle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer periodLength;
    private Integer cycleLength;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
