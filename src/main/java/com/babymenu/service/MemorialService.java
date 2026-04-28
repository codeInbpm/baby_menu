package com.babymenu.service;

import com.babymenu.entity.CoupleMemorial;
import java.util.List;

public interface MemorialService {
    void add(CoupleMemorial memorial);
    List<CoupleMemorial> list();
    void update(CoupleMemorial memorial);
    void delete(Long id);
    CoupleMemorial getMainMemorial();
}
