package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("game_info")
public class GameInfo implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String code;
    private String icon;
    private String type;
    private String url;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
}
