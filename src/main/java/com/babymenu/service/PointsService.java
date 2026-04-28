package com.babymenu.service;

import com.babymenu.dto.AllocateReqDTO;
import com.babymenu.dto.PointsInfoVO;
import com.babymenu.entity.PointsTransaction;

import java.util.List;

public interface PointsService {

    PointsInfoVO getInfo();

    List<PointsTransaction> getTransactions();

    void allocate(AllocateReqDTO req);

    void lazyResetPointsIfNeeded(Long userId);
}
