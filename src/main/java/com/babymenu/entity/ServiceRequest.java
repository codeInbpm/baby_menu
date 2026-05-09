package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("service_request")
public class ServiceRequest implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long coupleId;
    private Long fromUserId;
    private Long toUserId;
    /** JSON: [1,2,3] */
    private String itemIds;
    /** 文本：洗洗脚 + 按后背 */
    private String content;
    /** 0 待处理 1 已接受 2 已完成 3 已拒绝 */
    private Integer status;
    private LocalDateTime acceptTime;
    private LocalDateTime finishTime;
    private Integer score;
    private Integer petRewardPoints;
    private String petFeedback;
    private LocalDateTime evaluatedTime;
    private Integer isExemptionUsed;
    private Integer penaltyPoints;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
