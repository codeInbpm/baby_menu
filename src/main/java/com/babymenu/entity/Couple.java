package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("couple")
public class Couple implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userIdA;
    private Long userIdB;

    private Long petId;

    private Long ownerId;

    private Boolean switchRolePending;

    private Long switchRoleApplicant;
    private String albumCoverUrl;

    private LocalDateTime bindTime;
    /** 0 已绑定 1 已解绑 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
