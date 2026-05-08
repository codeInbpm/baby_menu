package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.dto.RequestCreateDTO;
import com.babymenu.dto.RequestEvaluateDTO;
import com.babymenu.entity.ServiceRequest;
import com.babymenu.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/request")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    /**
     * 发起点餐/服务请求
     *
     * @param dto 创建参数
     * @return 请求实体
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping
    public Result<ServiceRequest> create(@RequestBody RequestCreateDTO dto) {
        return Result.success(requestService.create(dto));
    }

    /**
     * 获取请求列表
     *
     * @param status 状态过滤(可选)
     * @return 请求列表
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping
    public Result<List<ServiceRequest>> list(@RequestParam(required = false) Integer status) {
        return Result.success(requestService.list(status));
    }

    /**
     * 获取请求详情
     *
     * @param id 请求ID
     * @return 请求详情
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/{id}")
    public Result<ServiceRequest> detail(@PathVariable Long id) {
        return Result.success(requestService.detail(id));
    }

    /**
     * 接受请求
     *
     * @param id 请求ID
     * @return 更新后的请求
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/{id}/accept")
    public Result<ServiceRequest> accept(@PathVariable Long id) {
        return Result.success(requestService.accept(id));
    }

    /**
     * 拒绝请求
     *
     * @param id 请求ID
     * @return 更新后的请求
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/{id}/reject")
    public Result<ServiceRequest> reject(@PathVariable Long id) {
        return Result.success(requestService.reject(id));
    }

    /**
     * 完成请求
     *
     * @param id 请求ID
     * @return 更新后的请求
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/{id}/finish")
    public Result<ServiceRequest> finish(@PathVariable Long id) {
        return Result.success(requestService.finish(id));
    }

    /**
     * 评价并奖励
     *
     * @param id  请求ID
     * @param dto 评价参数
     * @return 更新后的请求
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/{id}/evaluate")
    public Result<ServiceRequest> evaluate(@PathVariable Long id, @RequestBody RequestEvaluateDTO dto) {
        return Result.success(requestService.evaluate(id, dto));
    }
}
