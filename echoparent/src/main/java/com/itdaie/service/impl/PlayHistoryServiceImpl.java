package com.itdaie.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itdaie.common.exception.BusinessException;
import com.itdaie.mapper.MusicMapper;
import com.itdaie.mapper.PlayHistoryMapper;
import com.itdaie.mapper.UserMapper;
import com.itdaie.pojo.dto.PlayHistoryDTO;
import com.itdaie.pojo.entity.Music;
import com.itdaie.pojo.entity.PlayHistory;
import com.itdaie.pojo.entity.User;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.PlayHistoryVO;
import com.itdaie.service.DailyStatsService;
import com.itdaie.service.PlayHistoryService;
import com.itdaie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PlayHistoryServiceImpl implements PlayHistoryService {

    @Autowired
    private PlayHistoryMapper playHistoryMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private DailyStatsService dailyStatsService;

    @Override
    @Transactional
    public void record(PlayHistoryDTO dto) {
        if (dto == null) {
            throw new BusinessException("参数不能为空");
        }
        if (dto.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (dto.getSongId() == null) {
            throw new BusinessException("歌曲ID不能为空");
        }

        PlayHistory playHistory = new PlayHistory();
        playHistory.setUserId(dto.getUserId());
        playHistory.setSongId(dto.getSongId());

        playHistoryMapper.insert(playHistory);

        // 播放量 +1
        Music musicUpdate = new Music();
        musicUpdate.setId(dto.getSongId());
        musicMapper.update(
                musicUpdate,
                new LambdaUpdateWrapper<Music>()
                        .eq(Music::getId, dto.getSongId())
                        .setSql("play_count = play_count + 1")
        );

        // 热度增量：记录当日播放
        dailyStatsService.recordPlay("song", Long.valueOf(dto.getSongId()));

        // 根据听歌历史重新计算用户标签
        userService.recomputeUserTagsFromPlayHistory(dto.getUserId());
    }

    @Override
    public List<PlayHistoryVO> listByUser(Integer userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        return playHistoryMapper.selectByUserId(userId);
    }

    @Override
    public PageDataVo pageByUser(Integer userId, int pageNum, int pageSize) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (pageNum < 1) {
            throw new BusinessException("页码必须大于等于1");
        }
        if (pageSize < 1 || pageSize > 50) {
            throw new BusinessException("每页条数必须在1~50之间");
        }
        Page<PlayHistoryVO> page = new Page<>(pageNum, pageSize);
        Page<PlayHistoryVO> resultPage = playHistoryMapper.selectByUserIdPage(userId, page);
        List<PlayHistoryVO> records = resultPage.getRecords();

        // 二次查询填充作者名称
        fillAuthorNames(records);

        return new PageDataVo(resultPage.getTotal(), records);
    }

    /**
     * 根据 authorIds 批量查询用户，填充 authorNames
     */
    private void fillAuthorNames(List<PlayHistoryVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        // 收集所有 authorIds
        Set<Integer> allAuthorIds = new HashSet<>();
        for (PlayHistoryVO vo : records) {
            if (vo.getAuthorIds() != null) {
                allAuthorIds.addAll(vo.getAuthorIds());
            }
        }
        if (allAuthorIds.isEmpty()) {
            return;
        }
        // 批量查询用户
        List<User> users = userMapper.selectBatchIds(new ArrayList<>(allAuthorIds));
        Map<Integer, String> nameMap = new HashMap<>();
        for (User u : users) {
            if (u != null && u.getId() != null) {
                nameMap.put(u.getId(), StringUtils.hasText(u.getName()) ? u.getName().trim() : u.getUsername());
            }
        }
        // 填充 authorNames
        for (PlayHistoryVO vo : records) {
            if (vo.getAuthorIds() != null && !vo.getAuthorIds().isEmpty()) {
                List<String> names = new ArrayList<>();
                for (Integer aid : vo.getAuthorIds()) {
                    String name = nameMap.get(aid);
                    if (name != null) {
                        names.add(name);
                    }
                }
                vo.setAuthorNames(names);
            }
        }
    }

}
