package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("couple_memorial")
public class CoupleMemorial implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long coupleId;

    private String title;

    private LocalDate memorialDate;

    private String description;

    private Boolean isMain;

    private Integer calendarType;

    private Integer recordType;

    private Boolean isAnnualRemind;

    private LocalDateTime createTime;
}
