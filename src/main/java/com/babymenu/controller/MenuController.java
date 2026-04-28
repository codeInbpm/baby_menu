package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.entity.MenuCategory;
import com.babymenu.entity.MenuItem;
import com.babymenu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/category")
    public Result<List<MenuCategory>> categories() {
        return Result.success(menuService.listCategories());
    }

    @PostMapping("/category")
    public Result<MenuCategory> saveCategory(@RequestBody MenuCategory c) {
        return Result.success(menuService.saveCategory(c));
    }

    @DeleteMapping("/category/{id}")
    public Result<Void> removeCategory(@PathVariable Long id) {
        menuService.removeCategory(id);
        return Result.success();
    }

    @GetMapping("/item")
    public Result<List<MenuItem>> items(@RequestParam(required = false) Long categoryId) {
        return Result.success(menuService.listItems(categoryId));
    }

    @PostMapping("/item")
    public Result<MenuItem> saveItem(@RequestBody MenuItem item) {
        return Result.success(menuService.saveItem(item));
    }

    @DeleteMapping("/item/{id}")
    public Result<Void> removeItem(@PathVariable Long id) {
        menuService.removeItem(id);
        return Result.success();
    }
}
