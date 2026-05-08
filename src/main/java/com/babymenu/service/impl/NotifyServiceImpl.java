package com.babymenu.service.impl;

import com.babymenu.entity.Couple;
import com.babymenu.entity.User;
import com.babymenu.mapper.CoupleMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.NotifyService;
import com.babymenu.wechat.WechatSubscribeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final UserMapper userMapper;
    private final CoupleMapper coupleMapper;
    private final WechatSubscribeService subscribeService;

    @Override
    public void notifyPartner(User sender, String message, String pagePath) {
        if (sender.getCoupleId() == null) return;
        
        Couple couple = coupleMapper.selectById(sender.getCoupleId());
        if (couple == null) return;

        Long partnerId = couple.getUserIdA().equals(sender.getId()) ? couple.getUserIdB() : couple.getUserIdA();
        User partner = userMapper.selectById(partnerId);

        if (partner != null && partner.getOpenid() != null) {
            subscribeService.doSendGeneric(partner.getOpenid(), sender.getNickname(), message, pagePath);
        }
    }

    @Override
    public void sendActiveCardNotify(User sender, Long cardId) {
        if (sender.getCoupleId() == null) return;

        Couple couple = coupleMapper.selectById(sender.getCoupleId());
        if (couple == null) return;

        Long partnerId = couple.getUserIdA().equals(sender.getId()) ? couple.getUserIdB() : couple.getUserIdA();
        User partner = userMapper.selectById(partnerId);

        if (partner != null && partner.getOpenid() != null) {
            log.info("发送主动服务卡提醒: toOpenid={}, sender={}, cardId={}", partner.getOpenid(), sender.getNickname(), cardId);
            subscribeService.sendActiveServiceCardNotify(partner.getOpenid(), sender.getNickname(), cardId);
        }
    }
}
