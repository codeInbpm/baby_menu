package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("mall_avatar_frame")
public class MallAvatarFrame implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String styleDesc;
    private Integer price;
    private Integer isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
