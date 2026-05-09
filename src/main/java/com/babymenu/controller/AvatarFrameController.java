package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.service.AvatarFrameService;
import com.babymenu.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/avatar-frame")
@RequiredArgsConstructor
public class AvatarFrameController {

    private final AvatarFrameService avatarFrameService;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.success(avatarFrameService.getActiveFrames());
    }

    @GetMapping("/my")
    public Result<Map<String, Object>> my() {
        return Result.success(avatarFrameService.getMyFrames(UserContext.get()));
    }

    @PostMapping("/buy/{code}")
    public Result<?> buy(@PathVariable String code) {
        avatarFrameService.buyFrame(UserContext.get(), code);
        return Result.success();
    }

    @PostMapping("/equip")
    public Result<?> equip(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        avatarFrameService.equipFrame(UserContext.get(), code);
        return Result.success();
    }
}
