package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("couple_game_stats")
public class CoupleGameStats implements Serializable {
    private Long coupleId;
    private String gameCode;
    private Integer totalPlays;
    private Integer butlerWins;
    private Integer princessWins;
    private Long totalScore;
    private LocalDateTime lastPlayed;
}
