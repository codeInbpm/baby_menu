package com.babymenu.service;

import com.babymenu.dto.AllocateReqDTO;
import com.babymenu.dto.PointsInfoVO;
import com.babymenu.entity.PointsTransaction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface PointsService {

    PointsInfoVO getInfo();

    List<PointsTransaction> getTransactions();

    Page<PointsTransaction> getTransactionsPage(Integer current, Integer size, List<String> types);

    void allocate(AllocateReqDTO req);

    void lazyResetPointsIfNeeded(Long userId);

    void deductPoints(Long userId, Integer amount, String note);
}
