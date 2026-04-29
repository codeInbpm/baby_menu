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

    @PostMapping("/add")
    public Result<Void> add(@RequestBody FootprintReqDTO req) {
        footprintService.add(req);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<FootprintVO>> list() {
        return Result.success(footprintService.listAll());
    }

    @GetMapping("/{id}")
    public Result<FootprintVO> detail(@PathVariable Long id) {
        return Result.success(footprintService.detail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        footprintService.delete(id);
        return Result.success();
    }
}
