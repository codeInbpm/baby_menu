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

    /**
     * 获取经期概览（状态预测）
     *
     * @param targetUserId 目标用户ID(可选)
     * @return 概览信息
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/overview")
    public Result<PeriodService.PeriodOverviewVO> overview(@RequestParam(required = false) Long targetUserId) {
        Long uid = targetUserId != null ? targetUserId : UserContext.get();
        return Result.success(periodService.getOverview(uid));
    }

    /**
     * 获取经期日历记录
     *
     * @param yearMonth    年月 (yyyy-MM)
     * @param targetUserId 目标用户ID(可选)
     * @return 日历记录
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/calendar")
    public Result<List<PeriodRecord>> calendar(@RequestParam String yearMonth, @RequestParam(required = false) Long targetUserId) {
        Long uid = targetUserId != null ? targetUserId : UserContext.get();
        return Result.success(periodService.getCalendar(uid, yearMonth));
    }

    /**
     * 保存每日记录（经期/心情/症状）
     *
     * @param record 记录实体
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/record")
    public Result<Void> saveRecord(@RequestBody PeriodRecord record) {
        periodService.saveRecord(record);
        return Result.success();
    }

    /**
     * 获取经期配置
     *
     * @return 经期配置
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/config")
    public Result<PeriodConfig> getConfig() {
        return Result.success(periodService.getConfig(UserContext.get()));
    }

    /**
     * 保存经期配置
     *
     * @param config 经期配置
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/config")
    public Result<Void> saveConfig(@RequestBody PeriodConfig config) {
        periodService.saveConfig(config);
        return Result.success();
    }

    /**
     * 获取经期趋势分析
     *
     * @param targetUserId 目标用户ID(可选)
     * @return 分析结果
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/analysis")
    public Result<PeriodService.PeriodAnalysisVO> analysis(@RequestParam(required = false) Long targetUserId) {
        Long uid = targetUserId != null ? targetUserId : UserContext.get();
        return Result.success(periodService.getAnalysis(uid));
    }

    /**
     * 获取指定日期的关怀建议
     *
     * @param targetUserId 目标用户ID(可选)
     * @param date         日期 (yyyy-MM-dd)
     * @return 关怀建议
     */
    @GetMapping("/day-care")
    public Result<com.babymenu.dto.PeriodCareVO> dayCare(
            @RequestParam(required = false) Long targetUserId,
            @RequestParam String date) {
        Long uid = targetUserId != null ? targetUserId : UserContext.get();
        return Result.success(periodService.getCareDetail(uid, date));
    }
}
