package com.babymenu.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String code;
    private String nickname;
    private String avatar;
    private Integer gender;
}
