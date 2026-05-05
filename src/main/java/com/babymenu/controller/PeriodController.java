package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.entity.PeriodConfig;
import com.babymenu.entity.PeriodRecord;
import com.babymenu.service.PeriodService;
import com.babymenu.service.CoupleService;
import com.babymenu.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/period")
@RequiredArgsConstructor
public class PeriodController {

    private final PeriodService periodService;
    private final CoupleService coupleService;

    @GetMapping("/overview")
    public Result<PeriodService.PeriodOverviewVO> overview(@RequestParam(required = false) Long targetUserId) {
        Long uid = targetUserId != null ? targetUserId : UserContext.get();
        return Result.success(periodService.getOverview(uid));
    }

    @GetMapping("/calendar")
    public Result<List<PeriodRecord>> calendar(@RequestParam String yearMonth, @RequestParam(required = false) Long targetUserId) {
        Long uid = targetUserId != null ? targetUserId : UserContext.get();
        return Result.success(periodService.getCalendar(uid, yearMonth));
    }

    @PostMapping("/record")
    public Result<Void> saveRecord(@RequestBody PeriodRecord record) {
        periodService.saveRecord(record);
        return Result.success();
    }

    @GetMapping("/config")
    public Result<PeriodConfig> getConfig() {
        return Result.success(periodService.getConfig(UserContext.get()));
    }

    @PostMapping("/config")
    public Result<Void> saveConfig(@RequestBody PeriodConfig config) {
        periodService.saveConfig(config);
        return Result.success();
    }

    @GetMapping("/analysis")
    public Result<PeriodService.PeriodAnalysisVO> analysis(@RequestParam(required = false) Long targetUserId) {
        Long uid = targetUserId != null ? targetUserId : UserContext.get();
        return Result.success(periodService.getAnalysis(uid));
    }
}
