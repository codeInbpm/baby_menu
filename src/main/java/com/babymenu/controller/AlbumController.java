package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.entity.CoupleAlbum;
import com.babymenu.service.AlbumService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    /**
     * 批量上传相册图片
     *
     * @param dto 上传参数
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/upload")
    public Result<Void> uploadBatch(@RequestBody AlbumUploadDTO dto) {
        albumService.createBatch(dto.getImageUrls(), dto.getDescription());
        return Result.success(null);
    }

    /**
     * 获取相册列表
     *
     * @return 相册列表
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/list")
    public Result<List<CoupleAlbum>> list() {
        return Result.success(albumService.list());
    }

    /**
     * 设置相册封面
     *
     * @param id 相册ID
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/set-cover/{id}")
    public Result<Void> setCover(@PathVariable Long id) {
        albumService.setCover(id);
        return Result.success(null);
    }

    /**
     * 删除相册图片
     *
     * @param id 相册ID
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        albumService.delete(id);
        return Result.success(null);
    }

    @Data
    public static class AlbumUploadDTO {
        private List<String> imageUrls;
        private String description;
    }
}
