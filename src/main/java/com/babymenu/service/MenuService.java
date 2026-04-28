package com.babymenu.service;

import com.babymenu.entity.MenuCategory;
import com.babymenu.entity.MenuItem;
import java.util.List;

public interface MenuService {
    List<MenuCategory> listCategories();
    MenuCategory saveCategory(MenuCategory c);
    void removeCategory(Long id);

    List<MenuItem> listItems(Long categoryId);
    MenuItem saveItem(MenuItem item);
    void removeItem(Long id);
}
