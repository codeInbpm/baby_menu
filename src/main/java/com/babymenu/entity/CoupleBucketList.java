package com.babymenu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("couple_bucket_list")
public class CoupleBucketList implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long coupleId;

    private Long creatorId;

    private String title;

    private String description;

    private String category;

    private String coverUrl;

    private LocalDate targetDate;

    private Integer status; // 0: 进行中, 1: 已完成

    private Boolean ownerChecked;

    private Boolean petChecked;

    private LocalDateTime ownerCheckTime;

    private LocalDateTime petCheckTime;

    private LocalDateTime completeTime;

    private String memorialNoteOwner;

    private String memorialImageOwner;

    private String memorialNotePet;

    private String memorialImagePet;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
