package com.babymenu.service;

import java.util.List;
import java.util.Map;

public interface AvatarFrameService {
    List<Map<String, Object>> getActiveFrames();
    Map<String, Object> getMyFrames(Long userId);
    void buyFrame(Long userId, String frameCode);
    void equipFrame(Long userId, String frameCode);
}
