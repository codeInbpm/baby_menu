package com.babymenu.service;

import com.babymenu.entity.CoupleBucketList;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface BucketListService {

    void addWish(CoupleBucketList bucketList);

    Page<CoupleBucketList> getList(Integer current, Integer size, Integer status, String category, Integer year);

    CoupleBucketList getDetail(Long id);

    void checkWish(Long id);

    void updateMemorialNote(Long id, String note, String imageUrl);

    void deleteWish(Long id);
}
