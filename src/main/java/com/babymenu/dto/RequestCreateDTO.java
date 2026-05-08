package com.babymenu.dto;

import lombok.Data;
import java.util.List;

@Data
public class RequestCreateDTO {
    private List<Long> itemIds;
    private Boolean isFreeForPrincess;
    private Long cardId;
}
