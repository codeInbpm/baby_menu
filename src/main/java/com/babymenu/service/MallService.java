package com.babymenu.service;

import com.babymenu.entity.MallItem;
import com.babymenu.entity.UserInventory;

import java.util.List;
import java.util.Map;

public interface MallService {
    List<MallItem> getAvailableItems();
    void redeemItem(Long itemId);
    List<UserInventory> getMyInventory();
    void useItem(Long inventoryId, Map<String, Object> extraParams);
}
