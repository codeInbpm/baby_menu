package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.entity.CoupleBucketList;
import com.babymenu.service.BucketListService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bucket")
@RequiredArgsConstructor
public class BucketListController {

    private final BucketListService bucketListService;

    @PostMapping("/add")
    public Result<Void> addWish(@RequestBody CoupleBucketList bucketList) {
        bucketListService.addWish(bucketList);
        return Result.success(null);
    }

    @GetMapping("/list")
    public Result<Page<CoupleBucketList>> getList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        return Result.success(bucketListService.getList(current, size, status));
    }

    @GetMapping("/detail/{id}")
    public Result<CoupleBucketList> getDetail(@PathVariable Long id) {
        return Result.success(bucketListService.getDetail(id));
    }

    @PostMapping("/check/{id}")
    public Result<Void> checkWish(@PathVariable Long id) {
        bucketListService.checkWish(id);
        return Result.success(null);
    }

    @PostMapping("/note/{id}")
    public Result<Void> updateNote(@PathVariable Long id, @RequestBody Map<String, String> body) {
        bucketListService.updateMemorialNote(id, body.get("note"));
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteWish(@PathVariable Long id) {
        bucketListService.deleteWish(id);
        return Result.success(null);
    }
}
