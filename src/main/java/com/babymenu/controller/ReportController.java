package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.dto.ReportOverviewVO;
import com.babymenu.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/overview")
    public Result<ReportOverviewVO> getOverview(@RequestParam(defaultValue = "month") String type) {
        return Result.success(reportService.getOverview(type));
    }
}
