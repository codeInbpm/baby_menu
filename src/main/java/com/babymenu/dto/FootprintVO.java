package com.babymenu.dto;

import com.babymenu.entity.CoupleFootprint;
import com.babymenu.entity.FootprintMedia;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class FootprintVO extends CoupleFootprint {
    private List<FootprintMedia> medias;
}
