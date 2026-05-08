package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.entity.MallItem;
import com.babymenu.entity.UserInventory;
import com.babymenu.service.MallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mall")
@RequiredArgsConstructor
public class MallController {

    private final MallService mallService;

    @GetMapping("/items")
    public Result<List<MallItem>> getItems() {
        return Result.success(mallService.getAvailableItems());
    }

    @PostMapping("/redeem/{id}")
    public Result<Void> redeemItem(@PathVariable Long id) {
        mallService.redeemItem(id);
        return Result.success(null);
    }

    @GetMapping("/inventory")
    public Result<List<UserInventory>> getInventory() {
        return Result.success(mallService.getMyInventory());
    }

    @PostMapping("/use/{inventoryId}")
    public Result<Void> useItem(@PathVariable Long inventoryId, @RequestBody(required = false) Map<String, Object> extraParams) {
        mallService.useItem(inventoryId, extraParams);
        return Result.success(null);
    }

    @PostMapping("/remind/{inventoryId}")
    public Result<Void> sendPrincessReminder(@PathVariable Long inventoryId) {
        mallService.sendPrincessReminder(inventoryId);
        return Result.success(null);
    }
}
