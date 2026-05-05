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


    /**
     * 获取积分信息 (当前余额/今日限制)
     *
     * @return 积分信息
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/info")
    public Result<PointsInfoVO> getInfo() {
        return Result.success(pointsService.getInfo());
    }

    /**
     * 获取积分流水
     *
     * @return 积分流水列表
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/transactions")
    public Result<List<PointsTransaction>> getTransactions() {
        return Result.success(pointsService.getTransactions());
    }

    /**
     * 分配积分给对方 (点赞奖励)
     *
     * @param req 分配参数
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/allocate")
    public Result<Void> allocate(@RequestBody AllocateReqDTO req) {
        pointsService.allocate(req);
        return Result.success();
    }
}
