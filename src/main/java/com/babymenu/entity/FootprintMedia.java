package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("footprint_media")
public class FootprintMedia implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long footprintId;
    private Integer type; // 1:图片 2:视频
    private String url;
    private String description;
    private Integer sort;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableLogic
    private Integer deleted;
}
