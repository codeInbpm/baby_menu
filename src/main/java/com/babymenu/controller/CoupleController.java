package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.dto.BindDTO;
import com.babymenu.entity.Couple;
import com.babymenu.service.CoupleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/couple")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;

    @PostMapping("/invite")
    public Result<Map<String, String>> invite() {
        String code = coupleService.generateInvite();
        return Result.success(Map.of("code", code));
    }

    @PostMapping("/bind")
    public Result<Couple> bind(@RequestBody BindDTO dto) {
        return Result.success(coupleService.bindByCode(dto.getInviteCode()));
    }

    @PostMapping("/unbind")
    public Result<Void> unbind() {
        coupleService.unbind();
        return Result.success();
    }

    @PostMapping("/switch-role/request")
    public Result<Void> requestSwitchRole() {
        coupleService.requestSwitchRole();
        return Result.success();
    }

    @PostMapping("/switch-role/accept")
    public Result<Void> acceptSwitchRole() {
        coupleService.acceptSwitchRole();
        return Result.success();
    }
}
