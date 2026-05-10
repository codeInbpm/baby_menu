package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_settings")
public class UserSettings implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String currentAvatarFrameCode;
    private String currentBubbleStyle;
    private String currentSkinCode;
    private String unlockedSkins;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
