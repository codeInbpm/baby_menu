package com.babymenu.service;

import java.util.List;
import java.util.Map;

public interface SkinService {
    List<Map<String, Object>> getSkinList();
    void exchangeSkin(Long skinId);
    void setSkin(String skinCode);
    Map<String, Object> getCurrentSkin();
}
