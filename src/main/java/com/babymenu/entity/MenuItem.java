package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("menu_item")
public class MenuItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long coupleId;
    private Long categoryId;
    private String name;
    private String imageUrl;
    private String description;
    /** 时长，单位：分钟 */
    private Integer duration;
    private Integer pointsCost;
    private Integer sort;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
