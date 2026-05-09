package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.entity.UserConfessionRecord;
import com.babymenu.service.ConfessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/confession")
@RequiredArgsConstructor
public class ConfessionController {

    private final ConfessionService confessionService;

    @GetMapping("/pending")
    public Result<UserConfessionRecord> getPendingConfession() {
        return Result.success(confessionService.getPendingConfession());
    }

    @PostMapping("/submit/{id}")
    public Result<Void> submitConfession(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String content = body.get("content");
        String voiceUrl = body.get("voiceUrl");
        confessionService.submitConfession(id, content, voiceUrl);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<UserConfessionRecord>> getConfessionList() {
        return Result.success(confessionService.getConfessionList());
    }
}
