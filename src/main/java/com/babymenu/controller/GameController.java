package com.babymenu.controller;

import com.babymenu.common.Result;
import com.babymenu.entity.CoupleGameStats;
import com.babymenu.entity.GameInfo;
import com.babymenu.entity.GameRecord;
import com.babymenu.entity.User;
import com.babymenu.service.GameService;
import com.babymenu.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final UserService userService;

    @GetMapping("/list")
    public Result<List<GameInfo>> list() {
        return Result.success(gameService.listAllGames());
    }

    @PostMapping("/record")
    public Result<Void> saveRecord(@RequestBody GameRecord record) {
        gameService.saveRecord(record);
        return Result.success(null);
    }

    @GetMapping("/stats")
    public Result<List<CoupleGameStats>> stats() {
        User user = userService.currentUser();
        if (user.getCoupleId() == null) {
            return Result.success(new java.util.ArrayList<>());
        }
        return Result.success(gameService.getCoupleStats(user.getCoupleId()));
    }

    @GetMapping("/records/{gameCode}")
    public Result<List<GameRecord>> records(@PathVariable String gameCode) {
        User user = userService.currentUser();
        if (user.getCoupleId() == null) {
            return Result.success(new java.util.ArrayList<>());
        }
        return Result.success(gameService.getCoupleRecords(user.getCoupleId(), gameCode));
    }
}
