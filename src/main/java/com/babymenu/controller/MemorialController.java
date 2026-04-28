package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.entity.CoupleMemorial;
import com.babymenu.service.MemorialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/memorial")
@RequiredArgsConstructor
public class MemorialController {

    private final MemorialService memorialService;

    @PostMapping("/add")
    public Result<Void> add(@RequestBody CoupleMemorial memorial) {
        memorialService.add(memorial);
        return Result.success(null);
    }

    @GetMapping("/list")
    public Result<List<CoupleMemorial>> list() {
        return Result.success(memorialService.list());
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody CoupleMemorial memorial) {
        memorialService.update(memorial);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        memorialService.delete(id);
        return Result.success(null);
    }

    @GetMapping("/main")
    public Result<CoupleMemorial> getMainMemorial() {
        return Result.success(memorialService.getMainMemorial());
    }
}
