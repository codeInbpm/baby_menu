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

    @PostMapping
    public Result<ServiceRequest> create(@RequestBody RequestCreateDTO dto) {
        return Result.success(requestService.create(dto.getItemIds()));
    }

    @GetMapping
    public Result<List<ServiceRequest>> list(@RequestParam(required = false) Integer status) {
        return Result.success(requestService.list(status));
    }

    @GetMapping("/{id}")
    public Result<ServiceRequest> detail(@PathVariable Long id) {
        return Result.success(requestService.detail(id));
    }

    @PostMapping("/{id}/accept")
    public Result<ServiceRequest> accept(@PathVariable Long id) {
        return Result.success(requestService.accept(id));
    }

    @PostMapping("/{id}/reject")
    public Result<ServiceRequest> reject(@PathVariable Long id) {
        return Result.success(requestService.reject(id));
    }

    @PostMapping("/{id}/finish")
    public Result<ServiceRequest> finish(@PathVariable Long id) {
        return Result.success(requestService.finish(id));
    }

    @PostMapping("/{id}/evaluate")
    public Result<ServiceRequest> evaluate(@PathVariable Long id, @RequestBody RequestEvaluateDTO dto) {
        return Result.success(requestService.evaluate(id, dto));
    }
}
