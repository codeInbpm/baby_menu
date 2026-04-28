package com.babymenu.dto;

import com.babymenu.entity.User;
import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private User user;
    private Boolean bound;
}
