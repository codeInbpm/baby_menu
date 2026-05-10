package com.babymenu.service.impl;

import com.babymenu.entity.CoupleGameStats;
import com.babymenu.entity.GameInfo;
import com.babymenu.entity.GameRecord;
import com.babymenu.entity.User;
import com.babymenu.mapper.CoupleGameStatsMapper;
import com.babymenu.mapper.GameInfoMapper;
import com.babymenu.mapper.GameRecordMapper;
import com.babymenu.mapper.UserMapper;
import com.babymenu.service.GameService;
import com.babymenu.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private final GameInfoMapper gameInfoMapper;
    private final GameRecordMapper gameRecordMapper;
    private final CoupleGameStatsMapper coupleGameStatsMapper;
    private final UserMapper userMapper;
    private final UserService userService;

    @Override
    public List<GameInfo> listAllGames() {
        return gameInfoMapper.selectList(new LambdaQueryWrapper<GameInfo>()
                .eq(GameInfo::getStatus, 1)
                .orderByAsc(GameInfo::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRecord(GameRecord record) {
        User user = userService.currentUser();
        if (user.getCoupleId() == null) {
            throw new RuntimeException("请先绑定伴侣再进行游戏记录");
        }

        record.setInitiatorId(user.getId());
        record.setCoupleId(user.getCoupleId());
        if (record.getStartTime() == null) {
            record.setStartTime(LocalDateTime.now());
        }
        if (record.getEndTime() == null) {
            record.setEndTime(LocalDateTime.now());
        }
        gameRecordMapper.insert(record);

        // 更新统计数据
        LambdaQueryWrapper<CoupleGameStats> query = new LambdaQueryWrapper<CoupleGameStats>()
                .eq(CoupleGameStats::getCoupleId, user.getCoupleId())
                .eq(CoupleGameStats::getGameCode, record.getGameCode());
        CoupleGameStats stats = coupleGameStatsMapper.selectOne(query);

        boolean isNew = false;
        if (stats == null) {
            isNew = true;
            stats = new CoupleGameStats();
            stats.setCoupleId(user.getCoupleId());
            stats.setGameCode(record.getGameCode());
            stats.setTotalPlays(0);
            stats.setButlerWins(0);
            stats.setPrincessWins(0);
            stats.setTotalScore(0L);
        }

        stats.setTotalPlays(stats.getTotalPlays() + 1);
        stats.setLastPlayed(LocalDateTime.now());

        if (record.getWinnerId() != null) {
            User winner = userMapper.selectById(record.getWinnerId());
            if (winner != null && "butler".equals(winner.getRoleInCouple())) {
                stats.setButlerWins(stats.getButlerWins() + 1);
            } else if (winner != null && "pet".equals(winner.getRoleInCouple())) {
                // 原有的角色名是 pet (公主/宠爱对象)
                stats.setPrincessWins(stats.getPrincessWins() + 1);
            }
        }

        if (isNew) {
            coupleGameStatsMapper.insert(stats);
        } else {
            coupleGameStatsMapper.update(stats, query);
        }
    }

    @Override
    public List<CoupleGameStats> getCoupleStats(Long coupleId) {
        return coupleGameStatsMapper.selectList(new LambdaQueryWrapper<CoupleGameStats>()
                .eq(CoupleGameStats::getCoupleId, coupleId));
    }

    @Override
    public List<GameRecord> getCoupleRecords(Long coupleId, String gameCode) {
        return gameRecordMapper.selectList(new LambdaQueryWrapper<GameRecord>()
                .eq(GameRecord::getCoupleId, coupleId)
                .eq(GameRecord::getGameCode, gameCode)
                .orderByDesc(GameRecord::getEndTime)
                .last("limit 20"));
    }
}
