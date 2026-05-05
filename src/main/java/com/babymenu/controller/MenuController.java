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

    /**
     * 获取菜谱分类列表
     *
     * @return 分类列表
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/category")
    public Result<List<MenuCategory>> categories() {
        return Result.success(menuService.listCategories());
    }

    /**
     * 保存/更新菜谱分类
     *
     * @param c 分类实体
     * @return 保存结果
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/category")
    public Result<MenuCategory> saveCategory(@RequestBody MenuCategory c) {
        return Result.success(menuService.saveCategory(c));
    }

    /**
     * 删除菜谱分类
     *
     * @param id 分类ID
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @DeleteMapping("/category/{id}")
    public Result<Void> removeCategory(@PathVariable Long id) {
        menuService.removeCategory(id);
        return Result.success();
    }

    /**
     * 获取菜谱项列表
     *
     * @param categoryId 分类ID(可选)
     * @return 菜谱项列表
     * @author wb
     * @date 2026-05-05
     */
    @GetMapping("/item")
    public Result<List<MenuItem>> items(@RequestParam(required = false) Long categoryId) {
        return Result.success(menuService.listItems(categoryId));
    }

    /**
     * 保存/更新菜谱项
     *
     * @param item 菜谱项实体
     * @return 保存结果
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/item")
    public Result<MenuItem> saveItem(@RequestBody MenuItem item) {
        return Result.success(menuService.saveItem(item));
    }

    /**
     * 删除菜谱项
     *
     * @param id 菜谱项ID
     * @return Result
     * @author wb
     * @date 2026-05-05
     */
    @DeleteMapping("/item/{id}")
    public Result<Void> removeItem(@PathVariable Long id) {
        menuService.removeItem(id);
        return Result.success();
    }
}
