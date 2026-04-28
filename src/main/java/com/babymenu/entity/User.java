package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String unionid;
    private String nickname;
    private String avatar;
    /** 0 未知 1 男 2 女 */
    private Integer gender;
    private Long coupleId;
    private Integer dailyPointsLimit;
    private Integer points;
    private String roleInCouple;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
