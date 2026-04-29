package com.babymenu.service;

import com.babymenu.dto.TitleVO;
import java.util.List;

public interface TitleService {
    /** 获取用户所有称号（包含锁定和解锁的） */
    List<TitleVO> listUserTitles(Long userId);
    
    /** 佩戴称号 */
    void wearTitle(Long userId, String titleCode);
    
    /** 检查并解锁新称号 */
    void checkAndUnlock(Long userId);
    
    /** 获取当前佩戴的称号 */
    TitleVO getCurrentTitle(Long userId);
}
