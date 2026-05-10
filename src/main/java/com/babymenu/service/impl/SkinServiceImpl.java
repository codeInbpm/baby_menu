package com.babymenu.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.babymenu.common.BizException;
import com.babymenu.entity.MallSkin;
import com.babymenu.entity.User;
import com.babymenu.entity.UserSettings;
import com.babymenu.mapper.MallSkinMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.mapper.UserSettingsMapper;
import com.babymenu.service.PointsService;
import com.babymenu.service.SkinService;
import com.babymenu.util.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SkinServiceImpl implements SkinService {

    private final MallSkinMapper mallSkinMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final PointsService pointsService;
    private final UserMapper userMapper;

    private UserSettings getOrCreateSettings(Long userId) {
        UserSettings settings = userSettingsMapper.selectOne(new QueryWrapper<UserSettings>().eq("user_id", userId));
        if (settings == null) {
            settings = new UserSettings();
            settings.setUserId(userId);
            settings.setCurrentSkinCode("default");
            settings.setUnlockedSkins("[]");
            userSettingsMapper.insert(settings);
        }
        return settings;
    }

    private List<String> getUnlockedList(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return JSON.parseArray(json, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getSkinList() {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException(401, "未登录");
        
        UserSettings settings = getOrCreateSettings(userId);
        List<String> unlocked = getUnlockedList(settings.getUnlockedSkins());
        
        List<MallSkin> skins = mallSkinMapper.selectList(new QueryWrapper<MallSkin>().eq("is_active", 1).orderByAsc("price"));
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (MallSkin skin : skins) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", skin.getId());
            map.put("code", skin.getCode());
            map.put("name", skin.getName());
            map.put("price", skin.getPrice());
            map.put("previewImage", skin.getPreviewImage());
            map.put("configJson", JSON.parseObject(skin.getConfigJson()));
            map.put("unlocked", unlocked.contains(skin.getCode()));
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exchangeSkin(Long skinId) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException(401, "未登录");
        
        MallSkin skin = mallSkinMapper.selectById(skinId);
        if (skin == null || skin.getIsActive() != 1) {
            throw new BizException("皮肤不存在或已下架");
        }
        
        UserSettings settings = getOrCreateSettings(userId);
        List<String> unlocked = getUnlockedList(settings.getUnlockedSkins());
        if (unlocked.contains(skin.getCode())) {
            throw new BizException("您已经拥有该皮肤啦");
        }
        
        // 扣除积分
        pointsService.deductPoints(userId, skin.getPrice(), "兑换专属皮肤：" + skin.getName());
        
        // 更新解锁列表和当前皮肤
        unlocked.add(skin.getCode());
        settings.setUnlockedSkins(JSON.toJSONString(unlocked));
        settings.setCurrentSkinCode(skin.getCode());
        userSettingsMapper.updateById(settings);
    }

    @Override
    public void setSkin(String skinCode) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException(401, "未登录");
        
        UserSettings settings = getOrCreateSettings(userId);
        if ("default".equals(skinCode)) {
            settings.setCurrentSkinCode(skinCode);
            userSettingsMapper.updateById(settings);
            return;
        }
        
        List<String> unlocked = getUnlockedList(settings.getUnlockedSkins());
        if (!unlocked.contains(skinCode)) {
            throw new BizException("尚未解锁该皮肤");
        }
        
        settings.setCurrentSkinCode(skinCode);
        userSettingsMapper.updateById(settings);
    }

    @Override
    public Map<String, Object> getCurrentSkin() {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException(401, "未登录");
        
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(401, "用户不存在");

        Map<String, Object> res = new HashMap<>();
        
        // 公主角色不应用管家皮肤，直接返回默认
        if ("pet".equals(user.getRoleInCouple())) {
            res.put("code", "default");
            res.put("configJson", new HashMap<>());
            return res;
        }
        
        UserSettings settings = getOrCreateSettings(userId);
        String code = settings.getCurrentSkinCode();
        if (code == null || "default".equals(code)) {
            res.put("code", "default");
            res.put("configJson", new HashMap<>());
            return res;
        }
        
        MallSkin skin = mallSkinMapper.selectOne(new QueryWrapper<MallSkin>().eq("code", code));
        if (skin != null) {
            res.put("code", skin.getCode());
            res.put("configJson", JSON.parseObject(skin.getConfigJson()));
        } else {
            res.put("code", "default");
            res.put("configJson", new HashMap<>());
        }
        return res;
    }
}
