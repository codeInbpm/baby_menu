package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mall_item")
public class MallItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer price;
    private String description;
    private String icon;
    private Integer itemType; // 1-服务卡 2-免责金牌 3-头像框 4-告白券 5-皮肤
    private Integer validityDays;
    private Integer stock;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
