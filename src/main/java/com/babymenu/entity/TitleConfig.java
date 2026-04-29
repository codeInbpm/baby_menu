package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("title_config")
public class TitleConfig implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String titleCode;
    private String titleName;
    private String roleType; // owner, pet
    private String level; // normal, rare, legend
    private String description;
    private String unlockCondition; // JSON string
    private LocalDateTime createTime;
}
