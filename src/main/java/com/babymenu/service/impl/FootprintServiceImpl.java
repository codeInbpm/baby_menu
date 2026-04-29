package com.babymenu.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.babymenu.common.BizException;
import com.babymenu.util.UserContext;
import com.babymenu.dto.FootprintReqDTO;
import com.babymenu.dto.FootprintVO;
import com.babymenu.entity.CoupleFootprint;
import com.babymenu.entity.FootprintMedia;
import com.babymenu.entity.User;
import com.babymenu.mapper.CoupleFootprintMapper;
import com.babymenu.mapper.FootprintMediaMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.FootprintService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FootprintServiceImpl implements FootprintService {

    private final CoupleFootprintMapper footprintMapper;
    private final FootprintMediaMapper mediaMapper;
    private final UserMapper userMapper;

    private Long getCoupleId() {
        Long uid = UserContext.get();
        User user = userMapper.selectById(uid);
        if (user == null || user.getCoupleId() == null) {
            throw new BizException("需先绑定情侣关系哦");
        }
        return user.getCoupleId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(FootprintReqDTO req) {
        Long coupleId = getCoupleId();
        Long uid = UserContext.get();

        CoupleFootprint footprint = new CoupleFootprint();
        BeanUtil.copyProperties(req, footprint);
        footprint.setCoupleId(coupleId);
        footprint.setUserId(uid);
        if (req.getVisitDate() != null) {
            footprint.setVisitDate(LocalDate.parse(req.getVisitDate()));
        } else {
            footprint.setVisitDate(LocalDate.now());
        }
        
        footprintMapper.insert(footprint);

        if (CollUtil.isNotEmpty(req.getMedias())) {
            for (FootprintReqDTO.MediaDTO md : req.getMedias()) {
                FootprintMedia media = new FootprintMedia();
                BeanUtil.copyProperties(md, media);
                media.setFootprintId(footprint.getId());
                mediaMapper.insert(media);
            }
        }
    }

    @Override
    public List<FootprintVO> listAll() {
        Long coupleId = getCoupleId();
        List<CoupleFootprint> list = footprintMapper.selectList(Wrappers.<CoupleFootprint>lambdaQuery()
                .eq(CoupleFootprint::getCoupleId, coupleId)
                .orderByDesc(CoupleFootprint::getVisitDate)
                .orderByDesc(CoupleFootprint::getCreateTime));

        if (CollUtil.isEmpty(list)) return new ArrayList<>();

        List<Long> ids = list.stream().map(CoupleFootprint::getId).collect(Collectors.toList());
        List<FootprintMedia> medias = mediaMapper.selectList(Wrappers.<FootprintMedia>lambdaQuery()
                .in(FootprintMedia::getFootprintId, ids)
                .orderByAsc(FootprintMedia::getSort));

        Map<Long, List<FootprintMedia>> mediaMap = medias.stream()
                .collect(Collectors.groupingBy(FootprintMedia::getFootprintId));

        return list.stream().map(f -> {
            FootprintVO vo = new FootprintVO();
            BeanUtil.copyProperties(f, vo);
            vo.setMedias(mediaMap.getOrDefault(f.getId(), new ArrayList<>()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public FootprintVO detail(Long id) {
        Long coupleId = getCoupleId();
        CoupleFootprint f = footprintMapper.selectById(id);
        if (f == null || !f.getCoupleId().equals(coupleId)) {
            throw new BizException("找不到该足迹");
        }

        FootprintVO vo = new FootprintVO();
        BeanUtil.copyProperties(f, vo);

        List<FootprintMedia> medias = mediaMapper.selectList(Wrappers.<FootprintMedia>lambdaQuery()
                .eq(FootprintMedia::getFootprintId, id)
                .orderByAsc(FootprintMedia::getSort));
        vo.setMedias(medias);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long coupleId = getCoupleId();
        CoupleFootprint f = footprintMapper.selectById(id);
        if (f == null || !f.getCoupleId().equals(coupleId)) {
            throw new BizException("无权删除");
        }

        footprintMapper.deleteById(id);
        mediaMapper.delete(Wrappers.<FootprintMedia>lambdaQuery().eq(FootprintMedia::getFootprintId, id));
    }
}
