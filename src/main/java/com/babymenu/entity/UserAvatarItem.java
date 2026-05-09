package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_avatar_items")
public class UserAvatarItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String frameCode;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
