package com.babymenu.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.babymenu.common.BizException;
import com.babymenu.entity.MenuCategory;
import com.babymenu.entity.MenuItem;
import com.babymenu.entity.User;
import com.babymenu.mapper.MenuCategoryMapper;
import com.babymenu.mapper.MenuItemMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.MenuService;
import com.babymenu.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuCategoryMapper categoryMapper;
    private final MenuItemMapper itemMapper;
    private final UserMapper userMapper;

    private Long currentCoupleId() {
        Long uid = UserContext.get();
        User u = userMapper.selectById(uid);
        if (u == null || u.getCoupleId() == null) {
            throw new BizException("请先绑定伴侣");
        }
        return u.getCoupleId();
    }

    @Override
    public List<MenuCategory> listCategories() {
        Long cid = currentCoupleId();
        return categoryMapper.selectList(
                Wrappers.<MenuCategory>lambdaQuery()
                        .and(q -> q.eq(MenuCategory::getCoupleId, cid).or().eq(MenuCategory::getCoupleId, 0L))
                        .orderByAsc(MenuCategory::getSort));
    }

    @Override
    public MenuCategory saveCategory(MenuCategory c) {
        if (c.getId() != null) {
            MenuCategory old = categoryMapper.selectById(c.getId());
            if (old != null && old.getCoupleId() == 0L) {
                throw new BizException("系统公共分类不允许修改");
            }
        }
        
        c.setCoupleId(currentCoupleId());
        if (c.getId() == null) {
            if (c.getSort() == null) c.setSort(99);
            categoryMapper.insert(c);
        } else {
            categoryMapper.updateById(c);
        }
        return c;
    }

    @Override
    public void removeCategory(Long id) {
        MenuCategory old = categoryMapper.selectById(id);
        if (old != null && old.getCoupleId() == 0L) {
            throw new BizException("系统公共分类不允许删除");
        }
        currentCoupleId();
        categoryMapper.deleteById(id);
    }

    @Override
    public List<MenuItem> listItems(Long categoryId) {
        Long cid = currentCoupleId();
        return itemMapper.selectList(
                Wrappers.<MenuItem>lambdaQuery()
                        .and(q -> q.eq(MenuItem::getCoupleId, cid).or().eq(MenuItem::getCoupleId, 0L))
                        .eq(categoryId != null, MenuItem::getCategoryId, categoryId)
                        .orderByAsc(MenuItem::getSort));
    }

    @Override
    public MenuItem saveItem(MenuItem item) {
        if (item.getId() != null) {
            MenuItem old = itemMapper.selectById(item.getId());
            if (old != null && old.getCoupleId() == 0L) {
                throw new BizException("系统公共菜谱不允许修改");
            }
        }
        
        item.setCoupleId(currentCoupleId());
        if (item.getPointsCost() == null) item.setPointsCost(5);
        if (item.getSort() == null) item.setSort(99);
        if (item.getId() == null) {
            itemMapper.insert(item);
        } else {
            itemMapper.updateById(item);
        }
        return item;
    }

    @Override
    public void removeItem(Long id) {
        MenuItem old = itemMapper.selectById(id);
        if (old != null && old.getCoupleId() == 0L) {
            throw new BizException("系统公共菜谱不允许删除");
        }
        currentCoupleId();
        itemMapper.deleteById(id);
    }
}
