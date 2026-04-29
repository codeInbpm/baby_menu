package com.babymenu.service;

import com.babymenu.dto.ReportOverviewVO;

public interface ReportService {
    ReportOverviewVO getOverview(String type);
}
