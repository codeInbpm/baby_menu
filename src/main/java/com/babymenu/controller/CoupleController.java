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

    /**
     * 生成邀请码
     *
     * @return 邀请码
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/invite")
    public Result<Map<String, String>> invite() {
        String code = coupleService.generateInvite();
        return Result.success(Map.of("code", code));
    }

    /**
     * 绑定伴侣
     *
     * @param dto 绑定参数
     * @return 绑定结果
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/bind")
    public Result<Couple> bind(@RequestBody BindDTO dto) {
        return Result.success(coupleService.bindByCode(dto.getInviteCode(), dto.getRole()));
    }

    /**
     * 解除绑定
     *
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/unbind")
    public Result<Void> unbind() {
        coupleService.unbind();
        return Result.success();
    }

    /**
     * 申请角色互换
     *
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/switch-role/request")
    public Result<Void> requestSwitchRole() {
        coupleService.requestSwitchRole();
        return Result.success();
    }

    /**
     * 接受角色互换
     *
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/switch-role/accept")
    public Result<Void> acceptSwitchRole() {
        coupleService.acceptSwitchRole();
        return Result.success();
    }
}
