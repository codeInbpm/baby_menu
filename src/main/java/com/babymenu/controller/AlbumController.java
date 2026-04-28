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

    @PostMapping("/upload")
    public Result<Void> uploadBatch(@RequestBody AlbumUploadDTO dto) {
        albumService.createBatch(dto.getImageUrls(), dto.getDescription());
        return Result.success(null);
    }

    @GetMapping("/list")
    public Result<List<CoupleAlbum>> list() {
        return Result.success(albumService.list());
    }

    @PostMapping("/set-cover/{id}")
    public Result<Void> setCover(@PathVariable Long id) {
        albumService.setCover(id);
        return Result.success(null);
    }

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
