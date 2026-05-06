package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("period_record")
public class PeriodRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate recordDate;
    private Boolean isPeriod;
    private Integer flow;
    private String symptoms;
    private String mood;
    private String note;
    private Integer painLevel;
    private String specialNeeds;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
