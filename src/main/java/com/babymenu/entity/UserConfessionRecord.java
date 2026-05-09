package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_confession_records")
public class UserConfessionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long inventoryId;
    private Long ownerId;
    private Long petId;
    private String content;
    private String voiceUrl;
    
    /**
     * 0-待告白，1-已完成
     */
    private Integer status;
    
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}
