package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("couple_footprint")
public class CoupleFootprint implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long coupleId;
    private Long userId;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private LocalDate visitDate;
    private String description;
    private Boolean isSpecial;
    private String tags;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
