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

    /**
     * 添加纪念日
     *
     * @param memorial 纪念日实体
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody CoupleMemorial memorial) {
        memorialService.add(memorial);
        return Result.success(null);
    }

    /**
     * 获取纪念日列表
     *
     * @return 纪念日列表
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/list")
    public Result<List<CoupleMemorial>> list() {
        return Result.success(memorialService.list());
    }

    /**
     * 更新纪念日
     *
     * @param memorial 纪念日实体
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody CoupleMemorial memorial) {
        memorialService.update(memorial);
        return Result.success(null);
    }

    /**
     * 删除纪念日
     *
     * @param id 纪念日ID
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        memorialService.delete(id);
        return Result.success(null);
    }

    /**
     * 获取首页展示的纪念日
     *
     * @return 首页纪念日
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/main")
    public Result<CoupleMemorial> getMainMemorial() {
        return Result.success(memorialService.getMainMemorial());
    }
}
