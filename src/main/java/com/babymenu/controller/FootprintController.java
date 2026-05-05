package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.dto.FootprintReqDTO;
import com.babymenu.dto.FootprintVO;
import com.babymenu.service.FootprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/footprint")
@RequiredArgsConstructor
public class FootprintController {

    private final FootprintService footprintService;

    /**
     * 添加足迹打卡
     *
     * @param req 足迹参数
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody FootprintReqDTO req) {
        footprintService.add(req);
        return Result.success();
    }

    /**
     * 获取足迹列表
     *
     * @return 足迹列表
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/list")
    public Result<List<FootprintVO>> list() {
        return Result.success(footprintService.listAll());
    }

    /**
     * 获取足迹详情
     *
     * @param id 足迹ID
     * @return 足迹详情
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/{id}")
    public Result<FootprintVO> detail(@PathVariable Long id) {
        return Result.success(footprintService.detail(id));
    }

    /**
     * 删除足迹
     *
     * @param id 足迹ID
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        footprintService.delete(id);
        return Result.success();
    }
}
