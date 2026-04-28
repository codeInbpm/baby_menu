package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.dto.LoginDTO;
import com.babymenu.dto.LoginVO;
import com.babymenu.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }
}
