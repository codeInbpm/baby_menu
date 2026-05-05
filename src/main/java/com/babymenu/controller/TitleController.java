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

    /**
     * 获取用户称号列表
     *
     * @return 称号列表
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/list")
    public Result<List<TitleVO>> list() {
        return Result.success(titleService.listUserTitles(UserContext.get()));
    }

    /**
     * 佩戴称号
     *
     * @param code 称号代码
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/wear/{code}")
    public Result<Void> wear(@PathVariable String code) {
        titleService.wearTitle(UserContext.get(), code);
        return Result.success();
    }

    /**
     * 获取当前佩戴的称号
     *
     * @return 当前称号
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/current")
    public Result<TitleVO> current() {
        return Result.success(titleService.getCurrentTitle(UserContext.get()));
    }

    /**
     * 手动触发称号解锁检查
     *
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/check")
    public Result<Void> check() {
        titleService.checkAndUnlock(UserContext.get());
        return Result.success();
    }
}
