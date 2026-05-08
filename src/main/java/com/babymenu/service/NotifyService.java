package com.babymenu.service;

import com.babymenu.entity.User;

public interface NotifyService {
    /**
     * 给当前用户的伴侣发送订阅消息
     * @param sender 当前用户（发送者）
     * @param message 消息正文
     * @param pagePath 跳转页面
     */
    void notifyPartner(User sender, String message, String pagePath);

    /**
     * 发送主动服务卡提醒
     */
    void sendActiveCardNotify(User sender, Long cardId);
}
