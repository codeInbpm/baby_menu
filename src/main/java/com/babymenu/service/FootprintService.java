package com.babymenu.service;

import com.babymenu.dto.FootprintReqDTO;
import com.babymenu.dto.FootprintVO;

import java.util.List;

public interface FootprintService {
    void add(FootprintReqDTO req);
    List<FootprintVO> listAll();
    FootprintVO detail(Long id);
    void delete(Long id);
}
