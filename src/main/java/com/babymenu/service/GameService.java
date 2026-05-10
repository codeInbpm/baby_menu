package com.babymenu.service;

import com.babymenu.entity.GameInfo;
import com.babymenu.entity.GameRecord;
import com.babymenu.entity.CoupleGameStats;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface GameService {
    List<GameInfo> listAllGames();
    void saveRecord(GameRecord record);
    List<CoupleGameStats> getCoupleStats(Long coupleId);
    List<GameRecord> getCoupleRecords(Long coupleId, String gameCode);
}
