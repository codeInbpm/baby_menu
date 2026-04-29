package com.babymenu.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class FootprintReqDTO {
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private String visitDate; // yyyy-MM-dd
    private String description;
    private Boolean isSpecial;
    private String tags;
    private List<MediaDTO> medias;

    @Data
    public static class MediaDTO {
        private Integer type; // 1图片 2视频
        private String url;
        private String description;
        private Integer sort;
    }
}
