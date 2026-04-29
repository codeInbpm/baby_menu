package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.dto.TitleVO;
import com.babymenu.service.TitleService;
import com.babymenu.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/title")
@RequiredArgsConstructor
public class TitleController {

    private final TitleService titleService;

    @GetMapping("/list")
    public Result<List<TitleVO>> list() {
        return Result.success(titleService.listUserTitles(UserContext.get()));
    }

    @PostMapping("/wear/{code}")
    public Result<Void> wear(@PathVariable String code) {
        titleService.wearTitle(UserContext.get(), code);
        return Result.success();
    }

    @GetMapping("/current")
    public Result<TitleVO> current() {
        return Result.success(titleService.getCurrentTitle(UserContext.get()));
    }

    @PostMapping("/check")
    public Result<Void> check() {
        titleService.checkAndUnlock(UserContext.get());
        return Result.success();
    }
}
