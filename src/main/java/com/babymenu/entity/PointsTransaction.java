package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_transaction")
public class PointsTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long coupleId;

    /**
     * request_deduct: 请求扣除
     * allocate: 分配积分
     * daily_reset: 每日重置
     */
    private String type;

    private Integer amount;

    private Long relatedRequestId;

    private String note;

    private LocalDateTime createTime;
}
