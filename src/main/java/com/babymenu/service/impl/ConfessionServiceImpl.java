package com.babymenu.service.impl;

import com.babymenu.common.BizException;
import com.babymenu.entity.User;
import com.babymenu.entity.UserConfessionRecord;
import com.babymenu.mapper.UserConfessionRecordMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.ConfessionService;
import com.babymenu.util.UserContext;
import com.babymenu.wechat.WechatSubscribeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfessionServiceImpl implements ConfessionService {

    private final UserConfessionRecordMapper confessionMapper;
    private final UserMapper userMapper;
    private final WechatSubscribeService wechatSubscribeService;

    private User getValidUser() {
        Long uid = UserContext.get();
        if (uid == null) throw new BizException(401, "未登录");
        User user = userMapper.selectById(uid);
        if (user == null || user.getCoupleId() == null) {
            throw new BizException("请先绑定伴侣");
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerConfession(User owner, Long inventoryId) {
        if (owner.getCoupleId() == null) {
            throw new BizException("您还未绑定伴侣，无法使用告白券");
        }
        
        // 确保幂等
        Long count = confessionMapper.selectCount(new QueryWrapper<UserConfessionRecord>().eq("inventory_id", inventoryId));
        if (count > 0) {
            log.warn("告白记录已存在，跳过创建。inventoryId: {}", inventoryId);
            return;
        }

        User pet = userMapper.selectOne(new QueryWrapper<User>().eq("couple_id", owner.getCoupleId()).ne("id", owner.getId()));
        if (pet == null) {
            throw new BizException("找不到伴侣信息");
        }

        UserConfessionRecord record = new UserConfessionRecord();
        record.setInventoryId(inventoryId);
        record.setOwnerId(owner.getId());
        record.setPetId(pet.getId());
        record.setStatus(0); // 待告白
        record.setCreateTime(LocalDateTime.now());
        confessionMapper.insert(record);

        // 推送通知给公主
        try {
            wechatSubscribeService.sendConfessionRemindNotify(pet.getOpenid(), owner.getNickname());
        } catch (Exception e) {
            log.error("给公主发送告白券提醒失败", e);
        }
    }

    @Override
    public UserConfessionRecord getPendingConfession() {
        User pet = getValidUser();
        // 查找属于当前用户，且 status 为 0 的最新一条
        List<UserConfessionRecord> list = confessionMapper.selectList(new QueryWrapper<UserConfessionRecord>()
                .eq("pet_id", pet.getId())
                .eq("status", 0)
                .orderByDesc("create_time"));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitConfession(Long recordId, String content, String voiceUrl) {
        User pet = getValidUser();
        UserConfessionRecord record = confessionMapper.selectById(recordId);
        if (record == null) {
            throw new BizException("记录不存在");
        }
        if (!record.getPetId().equals(pet.getId())) {
            throw new BizException("无权操作此记录");
        }
        if (record.getStatus() == 1) {
            throw new BizException("告白已经提交过啦");
        }
        if ((content == null || content.trim().isEmpty()) && (voiceUrl == null || voiceUrl.trim().isEmpty())) {
            throw new BizException("告白文字或语音不能都为空哦");
        }

        record.setContent(content);
        record.setVoiceUrl(voiceUrl);
        record.setStatus(1); // 已完成
        record.setFinishTime(LocalDateTime.now());
        confessionMapper.updateById(record);

        // 获取管家信息
        User owner = userMapper.selectById(record.getOwnerId());
        if (owner != null && owner.getOpenid() != null) {
            try {
                wechatSubscribeService.sendConfessionReceivedNotify(owner.getOpenid(), pet.getNickname());
            } catch (Exception e) {
                log.error("给管家发送收到告白的提醒失败", e);
            }
        }
    }

    @Override
    public List<UserConfessionRecord> getConfessionList() {
        User user = getValidUser();
        // 查询管家或公主相关的已完成记录
        return confessionMapper.selectList(new QueryWrapper<UserConfessionRecord>()
                .and(w -> w.eq("owner_id", user.getId()).or().eq("pet_id", user.getId()))
                .eq("status", 1)
                .orderByDesc("finish_time"));
    }
}
