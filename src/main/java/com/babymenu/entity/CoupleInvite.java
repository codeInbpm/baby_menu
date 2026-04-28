package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("couple_invite")
public class CoupleInvite implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inviterId;
    private String inviteCode;
    private LocalDateTime expireTime;
    /** 0 未使用 1 已使用 2 已过期 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
