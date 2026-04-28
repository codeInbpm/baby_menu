package com.babymenu.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.babymenu.common.BizException;
import com.babymenu.entity.Couple;
import com.babymenu.entity.CoupleAlbum;
import com.babymenu.entity.User;
import com.babymenu.mapper.CoupleAlbumMapper;
import com.babymenu.mapper.CoupleMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.AlbumService;
import com.babymenu.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {

    private final CoupleAlbumMapper albumMapper;
    private final CoupleMapper coupleMapper;
    private final UserMapper userMapper;

    private User getValidUser() {
        Long uid = UserContext.get();
        User self = userMapper.selectById(uid);
        if (self == null || self.getCoupleId() == null) {
            throw new BizException("请先绑定伴侣");
        }
        return self;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBatch(List<String> imageUrls, String description) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new BizException("请至少上传一张照片");
        }
        User self = getValidUser();
        String batchId = IdUtil.simpleUUID();

        for (String url : imageUrls) {
            CoupleAlbum album = new CoupleAlbum();
            album.setCoupleId(self.getCoupleId());
            album.setUserId(self.getId());
            album.setBatchId(batchId);
            album.setImageUrl(url);
            album.setDescription(description);
            albumMapper.insert(album);
        }
    }

    @Override
    public List<CoupleAlbum> list() {
        User self = getValidUser();
        return albumMapper.selectList(Wrappers.<CoupleAlbum>lambdaQuery()
                .eq(CoupleAlbum::getCoupleId, self.getCoupleId())
                .orderByDesc(CoupleAlbum::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setCover(Long id) {
        User self = getValidUser();
        CoupleAlbum album = albumMapper.selectById(id);
        if (album == null || !album.getCoupleId().equals(self.getCoupleId())) {
            throw new BizException("照片不存在");
        }
        
        Couple couple = coupleMapper.selectById(self.getCoupleId());
        couple.setAlbumCoverUrl(album.getImageUrl());
        coupleMapper.updateById(couple);
    }

    @Override
    public void delete(Long id) {
        User self = getValidUser();
        CoupleAlbum album = albumMapper.selectById(id);
        if (album == null || !album.getCoupleId().equals(self.getCoupleId())) {
            throw new BizException("照片不存在");
        }
        albumMapper.deleteById(id);
    }
}
