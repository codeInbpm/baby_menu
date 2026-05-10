package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.service.SkinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mall/skin")
@RequiredArgsConstructor
public class SkinController {

    private final SkinService skinService;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getSkinList() {
        return Result.success(skinService.getSkinList());
    }

    @PostMapping("/exchange")
    public Result<Void> exchangeSkin(@RequestBody Map<String, Long> body) {
        Long skinId = body.get("skinId");
        if (skinId == null) return Result.error(400, "参数错误");
        skinService.exchangeSkin(skinId);
        return Result.success();
    }

    @PostMapping("/set")
    public Result<Void> setSkin(@RequestBody Map<String, String> body) {
        String skinCode = body.get("skinCode");
        if (skinCode == null) return Result.error(400, "参数错误");
        skinService.setSkin(skinCode);
        return Result.success();
    }

    @GetMapping("/current")
    public Result<Map<String, Object>> getCurrentSkin() {
        return Result.success(skinService.getCurrentSkin());
    }
}
