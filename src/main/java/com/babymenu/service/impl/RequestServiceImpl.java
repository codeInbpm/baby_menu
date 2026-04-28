package com.babymenu.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.babymenu.common.BizException;
import com.babymenu.entity.Couple;
import com.babymenu.entity.MenuItem;
import com.babymenu.entity.ServiceRequest;
import com.babymenu.entity.User;
import com.babymenu.mapper.CoupleMapper;
import com.babymenu.mapper.MenuItemMapper;
import com.babymenu.mapper.ServiceRequestMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.RequestService;
import com.babymenu.util.UserContext;
import com.babymenu.wechat.WechatSubscribeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final ServiceRequestMapper requestMapper;
    private final MenuItemMapper itemMapper;
    private final UserMapper userMapper;
    private final CoupleMapper coupleMapper;
    private final WechatSubscribeService subscribeService;

    @Override
    public ServiceRequest create(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) throw new BizException("请选择至少一项服务");
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null || self.getCoupleId() == null) throw new BizException("请先绑定伴侣");

        Couple couple = coupleMapper.selectById(self.getCoupleId());
        Long toId = couple.getUserIdA().equals(uid) ? couple.getUserIdB() : couple.getUserIdA();
        User to = userMapper.selectById(toId);

        List<MenuItem> items = itemMapper.selectBatchIds(itemIds);
        if (items.isEmpty()) throw new BizException("服务项不存在");
        String content = items.stream().map(MenuItem::getName).collect(Collectors.joining(" + "));

        ServiceRequest req = new ServiceRequest();
        req.setCoupleId(couple.getId());
        req.setFromUserId(uid);
        req.setToUserId(toId);
        req.setItemIds(JSONUtil.toJsonStr(itemIds));
        req.setContent(content);
        req.setStatus(0);
        requestMapper.insert(req);

        try {
            subscribeService.sendRequestNotify(to.getOpenid(), self.getNickname(), content, req.getId());
        } catch (Exception e) {
            log.warn("发送订阅消息失败: {}", e.getMessage());
        }
        return req;
    }

    @Override
    public List<ServiceRequest> list(Integer status) {
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null || self.getCoupleId() == null) return List.of();
        return requestMapper.selectList(
                Wrappers.<ServiceRequest>lambdaQuery()
                        .eq(ServiceRequest::getCoupleId, self.getCoupleId())
                        .eq(status != null, ServiceRequest::getStatus, status)
                        .orderByDesc(ServiceRequest::getCreateTime));
    }

    @Override
    public ServiceRequest detail(Long id) {
        ServiceRequest r = requestMapper.selectById(id);
        if (r == null) throw new BizException("请求不存在");
        return r;
    }

    @Override
    public ServiceRequest accept(Long id) {
        ServiceRequest r = mustOwn(id, true);
        if (r.getStatus() != 0) throw new BizException("当前状态无法接受");
        r.setStatus(1);
        r.setAcceptTime(LocalDateTime.now());
        requestMapper.updateById(r);
        return r;
    }

    @Override
    public ServiceRequest reject(Long id) {
        ServiceRequest r = mustOwn(id, true);
        if (r.getStatus() != 0) throw new BizException("当前状态无法拒绝");
        r.setStatus(3);
        requestMapper.updateById(r);
        return r;
    }

    @Override
    public ServiceRequest finish(Long id) {
        ServiceRequest r = mustOwn(id, true);
        if (r.getStatus() != 1) throw new BizException("请先接受请求");
        r.setStatus(2);
        r.setFinishTime(LocalDateTime.now());
        requestMapper.updateById(r);
        return r;
    }

    private ServiceRequest mustOwn(Long id, boolean asReceiver) {
        Long uid = UserContext.get();
        ServiceRequest r = requestMapper.selectById(id);
        if (r == null) throw new BizException("请求不存在");
        if (asReceiver && !r.getToUserId().equals(uid)) throw new BizException("无权操作");
        return r;
    }
}
