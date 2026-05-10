package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("game_record")
public class GameRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long coupleId;
    private String gameCode;
    private Long initiatorId;
    private Long winnerId;
    private String scoreJson;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer butlerRating;
    private Integer princessRating;
    private String comment;
}
