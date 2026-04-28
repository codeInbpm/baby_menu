package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.dto.AllocateReqDTO;
import com.babymenu.dto.PointsInfoVO;
import com.babymenu.entity.PointsTransaction;
import com.babymenu.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    @GetMapping("/info")
    public Result<PointsInfoVO> getInfo() {
        return Result.success(pointsService.getInfo());
    }

    @GetMapping("/transactions")
    public Result<List<PointsTransaction>> getTransactions() {
        return Result.success(pointsService.getTransactions());
    }

    @PostMapping("/allocate")
    public Result<Void> allocate(@RequestBody AllocateReqDTO req) {
        pointsService.allocate(req);
        return Result.success();
    }
}
