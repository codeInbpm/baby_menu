package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("mall_skin")
public class MallSkin implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private Integer price;
    private String previewImage;
    private Integer isActive;
    private String configJson;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
