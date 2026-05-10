package com.babymenu.service;

import com.babymenu.entity.User;
import com.babymenu.entity.UserConfessionRecord;
import java.util.List;

public interface ConfessionService {
    
    /**
     * 管家兑换告白券后立刻触发
     */
    void triggerConfession(User owner, Long inventoryId);
    
    /**
     * 获取公主待处理的告白任务
     */
    UserConfessionRecord getPendingConfession();
    
    /**
     * 公主提交告白
     */
    void submitConfession(Long recordId, String content, String voiceUrl);
    
    /**
     * 获取历史告白记录列表
     */
    List<UserConfessionRecord> getConfessionList();
}
