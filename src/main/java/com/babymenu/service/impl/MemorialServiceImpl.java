package com.babymenu.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.babymenu.common.BizException;
import com.babymenu.entity.CoupleMemorial;
import com.babymenu.entity.User;
import com.babymenu.mapper.CoupleMemorialMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.MemorialService;
import com.babymenu.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemorialServiceImpl implements MemorialService {

    private final CoupleMemorialMapper memorialMapper;
    private final UserMapper userMapper;

    private User getValidUser() {
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null || self.getCoupleId() == null) {
            throw new BizException("请先绑定伴侣");
        }
        return self;
    }

    private void handleMainReset(Long coupleId) {
        // 如果新增了 is_main = true，先把其他的设为 false
        var list = memorialMapper.selectList(Wrappers.<CoupleMemorial>lambdaQuery()
                .eq(CoupleMemorial::getCoupleId, coupleId)
                .eq(CoupleMemorial::getIsMain, true));
        for (CoupleMemorial mem : list) {
            mem.setIsMain(false);
            memorialMapper.updateById(mem);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(CoupleMemorial memorial) {
        User self = getValidUser();
        memorial.setUserId(self.getId());
        memorial.setCoupleId(self.getCoupleId());
        if (memorial.getIsMain() != null && memorial.getIsMain()) {
            handleMainReset(self.getCoupleId());
        }
        memorialMapper.insert(memorial);
    }

    @Override
    public List<CoupleMemorial> list() {
        User self = getValidUser();
        // 按照日期排序
        return memorialMapper.selectList(Wrappers.<CoupleMemorial>lambdaQuery()
                .eq(CoupleMemorial::getCoupleId, self.getCoupleId())
                .orderByDesc(CoupleMemorial::getMemorialDate));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CoupleMemorial memorial) {
        User self = getValidUser();
        CoupleMemorial exist = memorialMapper.selectById(memorial.getId());
        if (exist == null || !exist.getCoupleId().equals(self.getCoupleId())) {
            throw new BizException("纪念日不存在");
        }
        if (memorial.getIsMain() != null && memorial.getIsMain() && !exist.getIsMain()) {
            handleMainReset(self.getCoupleId());
        }
        memorialMapper.updateById(memorial);
    }

    @Override
    public void delete(Long id) {
        User self = getValidUser();
        CoupleMemorial exist = memorialMapper.selectById(id);
        if (exist == null || !exist.getCoupleId().equals(self.getCoupleId())) {
            throw new BizException("纪念日不存在");
        }
        memorialMapper.deleteById(id);
    }

    @Override
    public CoupleMemorial getMainMemorial() {
        User self = getValidUser();
        return memorialMapper.selectOne(Wrappers.<CoupleMemorial>lambdaQuery()
                .eq(CoupleMemorial::getCoupleId, self.getCoupleId())
                .eq(CoupleMemorial::getIsMain, true)
                .last("LIMIT 1"));
    }
}
