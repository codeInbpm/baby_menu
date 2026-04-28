package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.entity.User;
import com.babymenu.service.CoupleService;
import com.babymenu.service.MemorialService;
import com.babymenu.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CoupleService coupleService;
    private final MemorialService memorialService;

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        User self = userService.currentUser();
        User partner = coupleService.partner();
        Map<String, Object> m = new HashMap<>();
        m.put("user", self);
        m.put("partner", partner);
        m.put("couple", coupleService.getCoupleInfo());
        m.put("bound", self.getCoupleId() != null);
        try {
            m.put("mainMemorial", memorialService.getMainMemorial());
        } catch (Exception e) {
            m.put("mainMemorial", null);
        }
        return Result.success(m);
    }
}
