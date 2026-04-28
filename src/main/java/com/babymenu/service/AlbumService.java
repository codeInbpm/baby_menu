package com.babymenu.service;

import com.babymenu.entity.CoupleAlbum;
import java.util.List;

public interface AlbumService {
    void createBatch(List<String> imageUrls, String description);
    List<CoupleAlbum> list();
    void setCover(Long id);
    void delete(Long id);
}
