package com.itdaie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itdaie.common.exception.BusinessException;
import com.itdaie.common.util.SortUtils;
import com.itdaie.common.util.TagUtils;
import com.itdaie.mapper.MusicMapper;
import com.itdaie.mapper.PlaylistMapper;
import com.itdaie.mapper.UserMapper;
import com.itdaie.pojo.dto.PlaylistDTO;
import com.itdaie.pojo.dto.PlaylistPageDTO;
import com.itdaie.pojo.entity.Music;
import com.itdaie.pojo.entity.Playlist;
import com.itdaie.pojo.entity.User;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.PlaylistVO;
import com.itdaie.service.DailyStatsService;
import com.itdaie.service.PlaylistService;
import com.itdaie.utils.OssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class PlaylistServiceImpl implements PlaylistService {

    /**
     * 允许的排序字段白名单
     */
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("collect_count", "play_count", "hot", "create_time", "update_time");

    private static final Map<String, SFunction<Playlist, ?>> SORT_FIELD_MAP = buildSortFieldMap();

    /**
     * PostgreSQL数组查询谓词模板（模糊匹配）
     */
    private static final String PRED_EMO_TAGS =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(emo_tags, '{}'::text[])) AS u WHERE u ILIKE CONCAT('%', {0}, '%'))";
    private static final String PRED_INTEREST_TAGS =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(interest_tags, '{}'::text[])) AS u WHERE u ILIKE CONCAT('%', {0}, '%'))";

    /**
     * PostgreSQL数组精确匹配谓词模板（全局搜索）
     */
    private static final String PRED_EMO_TAGS_EXACT =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(emo_tags, '{}'::text[])) AS t WHERE t = {0})";
    private static final String PRED_INTEREST_TAGS_EXACT =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(interest_tags, '{}'::text[])) AS t WHERE t = {0})";

    @Autowired
    private PlaylistMapper playlistMapper;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private DailyStatsService dailyStatsService;

    @Override
    public PageDataVo pageQuery(PlaylistPageDTO dto) {
        PlaylistPageDTO safeDto = normalizeDto(dto);
        String sortBy = safeDto.getSortBy();
        String sortOrder = safeDto.getSortOrder();

        SortUtils.validateSort(sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Page<Playlist> page = new Page<>(safeDto.getPageNum(), safeDto.getPageSize());
        LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<>();

        // 基础等值条件（使用索引：idx_playlists_user_id）
        wrapper.eq(safeDto.getUserId() != null, Playlist::getUserId, safeDto.getUserId())
                .eq(safeDto.getIsPrivate() != null, Playlist::getIsPrivate, safeDto.getIsPrivate())
                .eq(safeDto.getIsLike() != null, Playlist::getIsLike, safeDto.getIsLike())
                .eq(safeDto.getIsRecommended() != null, Playlist::getIsRecommended, safeDto.getIsRecommended());

        // 模糊查询条件
        wrapper.like(StringUtils.hasText(safeDto.getPlaylistName()), Playlist::getPlaylistName, safeDto.getPlaylistName())
                .like(StringUtils.hasText(safeDto.getUserName()), Playlist::getUserName, safeDto.getUserName());

        // 歌曲ID数组过滤（使用索引：idx_playlists_song_ids）
        if (!CollectionUtils.isEmpty(safeDto.getSongIds())) {
            // 使用 && 操作符检查两个数组是否有交集（歌单包含任一指定歌曲即可匹配）
            wrapper.apply("song_ids && {0}::integer[]", (Object) safeDto.getSongIds().toArray(new Integer[0]));
        }

        // 标签数组模糊查询（使用索引：idx_playlists_emo_tags / idx_playlists_interest_tags）
        applyArrayKeywordOrGroup(wrapper, safeDto.getEmoTags(), PRED_EMO_TAGS);
        applyArrayKeywordOrGroup(wrapper, safeDto.getInterestTags(), PRED_INTEREST_TAGS);

        // 排序构建
        SortUtils.buildSort(sortBy, sortOrder, wrapper, SORT_FIELD_MAP);

        Page<Playlist> resultPage = playlistMapper.selectPage(page, wrapper);
        List<PlaylistVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        return new PageDataVo(resultPage.getTotal(), records);
    }

    /**
     * 应用数组关键词OR条件组
     * 多个关键词之间为OR关系：命中任一即满足
     */
    private void applyArrayKeywordOrGroup(LambdaQueryWrapper<Playlist> wrapper, List<String> keywords,
                                          String predSql) {
        if (keywords == null || keywords.isEmpty()) {
            return;
        }
        List<String> parts = keywords.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        if (parts.isEmpty()) {
            return;
        }
        if (parts.size() == 1) {
            wrapper.apply(predSql, parts.get(0));
            return;
        }
        wrapper.and(w -> {
            w.apply(predSql, parts.get(0));
            for (int i = 1; i < parts.size(); i++) {
                w.or().apply(predSql, parts.get(i));
            }
        });
    }

    /**
     * 标准化并校验分页参数
     */
    private PlaylistPageDTO normalizeDto(PlaylistPageDTO dto) {
        if (dto == null) {
            throw new BusinessException("request body is required");
        }
        if (dto.getPageNum() == null || dto.getPageNum() < 1) {
            throw new BusinessException("pageNum must be >= 1");
        }
        if (dto.getPageSize() == null || dto.getPageSize() < 1) {
            throw new BusinessException("pageSize must be >= 1");
        }
        if (dto.getPageSize() > 200) {
            throw new BusinessException("pageSize must be <= 200");
        }
        dto.setSortBy(SortUtils.normalizeSortBy(dto.getSortBy()));
        dto.setSortOrder(SortUtils.normalizeSortOrder(dto.getSortBy(), dto.getSortOrder()));
        return dto;
    }

    /**
     * 构建排序字段映射表
     */
    private static Map<String, SFunction<Playlist, ?>> buildSortFieldMap() {
        Map<String, SFunction<Playlist, ?>> fieldMap = new HashMap<>();
        fieldMap.put("collect_count", Playlist::getCollectCount);
        fieldMap.put("play_count", Playlist::getPlayCount);
        fieldMap.put("hot", Playlist::getHot);
        fieldMap.put("create_time", Playlist::getCreateTime);
        fieldMap.put("update_time", Playlist::getUpdateTime);
        return fieldMap;
    }

    /**
     * 实体转换为视图对象
     */
    private PlaylistVO convertToVO(Playlist playlist) {
        PlaylistVO vo = new PlaylistVO();
        vo.setId(playlist.getId());
        vo.setPlaylistName(playlist.getPlaylistName());
        vo.setUserId(playlist.getUserId());
        vo.setUserName(playlist.getUserName());
        vo.setIsPrivate(playlist.getIsPrivate());
        vo.setListDescription(playlist.getListDescription());
        vo.setSongIds(playlist.getSongIds());
        vo.setEmoTags(playlist.getEmoTags());
        vo.setInterestTags(playlist.getInterestTags());
        vo.setCollectCount(playlist.getCollectCount());
        vo.setPlayCount(playlist.getPlayCount());
        vo.setIsLike(playlist.getIsLike());
        vo.setHot(playlist.getHot());
        vo.setCommentCount(playlist.getCommentCount());
        vo.setIsRecommended(playlist.getIsRecommended());
        vo.setImageUrl(playlist.getImageUrl());
        vo.setCreateTime(playlist.getCreateTime());
        vo.setUpdateTime(playlist.getUpdateTime());
        return vo;
    }

    @Override
    public PlaylistVO getById(Integer id) {
        if (id == null) {
            throw new BusinessException("歌单ID不能为空");
        }
        Playlist playlist = playlistMapper.selectById(id);
        if (playlist == null) {
            throw new BusinessException("歌单不存在");
        }
        PlaylistVO vo = convertToVO(playlist);
        // 若歌单表中 user_name 为空，则根据 user_id 查询 users 表补全
        if (!StringUtils.hasText(vo.getUserName()) && playlist.getUserId() != null) {
            User user = userMapper.selectById(playlist.getUserId());
            if (user != null && StringUtils.hasText(user.getName())) {
                vo.setUserName(user.getName());
            }
        }
        return vo;
    }

    @Override
    @Transactional
    public void add(PlaylistDTO dto) {
        validateAddDTO(dto);

        Playlist playlist = new Playlist();
        playlist.setPlaylistName(dto.getPlaylistName().trim());
        playlist.setUserId(dto.getUserId());
        playlist.setUserName(dto.getUserName());
        playlist.setIsPrivate(dto.getIsPrivate() != null ? dto.getIsPrivate() : false);
        playlist.setListDescription(dto.getListDescription());
        playlist.setSongIds(dto.getSongIds() != null ? dto.getSongIds() : List.of());
        playlist.setEmoTags(dto.getEmoTags() != null ? dto.getEmoTags() : List.of());
        playlist.setInterestTags(dto.getInterestTags() != null ? dto.getInterestTags() : List.of());
        playlist.setIsLike(dto.getIsLike() != null ? dto.getIsLike() : false);
        playlist.setIsRecommended(dto.getIsRecommended() != null ? dto.getIsRecommended() : false);
        playlist.setImageUrl(dto.getImageUrl());

        // 初始化计数字段
        playlist.setCollectCount(0);
        playlist.setPlayCount(0);
        playlist.setHot(0);
        playlist.setCommentCount(0);

        playlistMapper.insert(playlist);
    }

    @Override
    @Transactional
    public void update(PlaylistDTO dto) {
        validateUpdateDTO(dto);

        Integer id = dto.getId();
        Playlist existingPlaylist = playlistMapper.selectById(id);
        if (existingPlaylist == null) {
            throw new BusinessException("歌单不存在");
        }

        Playlist playlist = new Playlist();
        playlist.setId(id);

        if (StringUtils.hasText(dto.getPlaylistName())) {
            playlist.setPlaylistName(dto.getPlaylistName().trim());
        }
        if (dto.getUserId() != null) {
            playlist.setUserId(dto.getUserId());
        }
        if (dto.getUserName() != null) {
            playlist.setUserName(dto.getUserName());
        }
        if (dto.getIsPrivate() != null) {
            playlist.setIsPrivate(dto.getIsPrivate());
        }
        if (dto.getListDescription() != null) {
            playlist.setListDescription(dto.getListDescription());
        }
        if (dto.getSongIds() != null) {
            playlist.setSongIds(dto.getSongIds());
        }
        if (dto.getEmoTags() != null) {
            playlist.setEmoTags(dto.getEmoTags());
        }
        if (dto.getInterestTags() != null) {
            playlist.setInterestTags(dto.getInterestTags());
        }
        if (dto.getIsLike() != null) {
            playlist.setIsLike(dto.getIsLike());
        }
        if (dto.getIsRecommended() != null) {
            playlist.setIsRecommended(dto.getIsRecommended());
        }
        if (dto.getImageUrl() != null) {
            playlist.setImageUrl(dto.getImageUrl());
        }

        playlistMapper.updateById(playlist);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException("歌单ID不能为空");
        }
        Playlist existingPlaylist = playlistMapper.selectById(id);
        if (existingPlaylist == null) {
            throw new BusinessException("歌单不存在");
        }
        if (Boolean.TRUE.equals(existingPlaylist.getIsLike())) {
            throw new BusinessException("系统歌单'我的喜欢'不可删除");
        }
        ossUtil.deleteByPublicUrl(existingPlaylist.getImageUrl());
        playlistMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("删除ID列表不能为空");
        }
        List<Integer> validIds = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (validIds.isEmpty()) {
            throw new BusinessException("删除ID列表不能为空");
        }
        List<Playlist> playlists = playlistMapper.selectBatchIds(validIds);
        for (Playlist p : playlists) {
            ossUtil.deleteByPublicUrl(p.getImageUrl());
        }
        playlistMapper.deleteByIds(validIds);
    }

    @Override
    public List<PlaylistVO> homeRecommend(Integer userId) {
        Set<String> allTags = new LinkedHashSet<>();
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                if (user.getEmoTags() != null) {
                    allTags.addAll(user.getEmoTags());
                }
                if (user.getInterestTags() != null) {
                    allTags.addAll(user.getInterestTags());
                }
            }
        }

        List<PlaylistVO> result = new ArrayList<>();
        Set<Integer> selectedIds = new LinkedHashSet<>();

        // 标签匹配查询
        if (!allTags.isEmpty()) {
            LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Playlist::getIsPrivate, false)
                    .and(w -> w.apply("emo_tags && {0}::text[]", (Object) allTags.toArray(new String[0]))
                            .or().apply("interest_tags && {0}::text[]", (Object) allTags.toArray(new String[0])))
                    .orderByDesc(Playlist::getHot)
                    .last("LIMIT 5");
            List<Playlist> matched = playlistMapper.selectList(wrapper);
            for (Playlist p : matched) {
                result.add(convertToVO(p));
                selectedIds.add(p.getId());
            }
        }

        // 补全到5个
        if (result.size() < 5) {
            LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Playlist::getIsPrivate, false);
            if (!selectedIds.isEmpty()) {
                wrapper.notIn(Playlist::getId, selectedIds);
            }
            wrapper.orderByDesc(Playlist::getHot)
                    .last("LIMIT " + (5 - result.size()));
            List<Playlist> fill = playlistMapper.selectList(wrapper);
            for (Playlist p : fill) {
                result.add(convertToVO(p));
            }
        }

        return result;
    }

    @Override
    public PageDataVo searchPlaylists(String keyword, int pageNum, int pageSize) {
        if (!StringUtils.hasText(keyword)) {
            return new PageDataVo(0L, List.of());
        }
        String k = keyword.trim();
        Page<Playlist> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Playlist::getIsPrivate, false)
                .and(w -> {
                    w.like(Playlist::getPlaylistName, k)
                            .or().apply(PRED_EMO_TAGS_EXACT, k)
                            .or().apply(PRED_INTEREST_TAGS_EXACT, k);
                });

        wrapper.orderByDesc(Playlist::getHot);

        Page<Playlist> resultPage = playlistMapper.selectPage(page, wrapper);
        List<PlaylistVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        return new PageDataVo(resultPage.getTotal(), records);
    }

    /**
     * 校验新增DTO
     */
    private void validateAddDTO(PlaylistDTO dto) {
        if (dto == null) {
            throw new BusinessException("歌单信息不能为空");
        }
        if (!StringUtils.hasText(dto.getPlaylistName())) {
            throw new BusinessException("歌单名称不能为空");
        }
        if (dto.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
    }

    @Override
    @Transactional
    public List<Integer> addSongToPlaylist(Integer playlistId, Integer musicId, Integer operatorUserId) {
        if (playlistId == null || playlistId <= 0) {
            throw new BusinessException("歌单ID不能为空");
        }
        if (musicId == null || musicId <= 0) {
            throw new BusinessException("音乐ID不能为空");
        }
        if (operatorUserId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) {
            throw new BusinessException("歌单不存在");
        }
        if (!operatorUserId.equals(playlist.getUserId())) {
            throw new BusinessException("无权操作该歌单");
        }

        List<Integer> songIds = playlist.getSongIds() != null ? new ArrayList<>(playlist.getSongIds()) : new ArrayList<>();
        if (songIds.contains(musicId)) {
            throw new BusinessException("该歌曲已在歌单中");
        }
        songIds.add(musicId);

        // 根据歌单内全部歌曲重新计算标签
        List<Music> allMusics = musicMapper.selectBatchIds(songIds);
        List<String> emoTags = TagUtils.computeTopTagsWithTies(
                allMusics.stream().map(Music::getEmoTags).toList(), 2);
        List<String> interestTags = TagUtils.computeTopTagsWithTies(
                allMusics.stream().map(Music::getInterestTags).toList(), 2);

        Playlist update = new Playlist();
        update.setId(playlistId);
        update.setSongIds(songIds);
        update.setEmoTags(emoTags);
        update.setInterestTags(interestTags);
        playlistMapper.updateById(update);

        // 收藏数 +1
        Music musicUpdate = new Music();
        musicUpdate.setId(musicId);
        musicMapper.update(
                musicUpdate,
                new LambdaUpdateWrapper<Music>()
                        .eq(Music::getId, musicId)
                        .setSql("collect_count = collect_count + 1")
        );

        // 热度增量：记录当日收藏
        dailyStatsService.recordCollect("song", Long.valueOf(musicId));

        return syncCollectMusicIds(operatorUserId);
    }

    @Override
    @Transactional
    public List<Integer> removeSongFromPlaylist(Integer playlistId, Integer musicId, Integer operatorUserId) {
        if (playlistId == null || playlistId <= 0) {
            throw new BusinessException("歌单ID不能为空");
        }
        if (musicId == null || musicId <= 0) {
            throw new BusinessException("音乐ID不能为空");
        }
        if (operatorUserId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) {
            throw new BusinessException("歌单不存在");
        }
        if (!operatorUserId.equals(playlist.getUserId())) {
            throw new BusinessException("无权操作该歌单");
        }

        List<Integer> songIds = playlist.getSongIds() != null ? new ArrayList<>(playlist.getSongIds()) : new ArrayList<>();
        if (!songIds.remove(musicId)) {
            throw new BusinessException("该歌曲不在歌单中");
        }

        // 根据歌单内剩余歌曲重新计算标签
        List<Music> remainingMusics = musicMapper.selectBatchIds(songIds);
        List<String> emoTags = TagUtils.computeTopTagsWithTies(
                remainingMusics.stream().map(Music::getEmoTags).toList(), 2);
        List<String> interestTags = TagUtils.computeTopTagsWithTies(
                remainingMusics.stream().map(Music::getInterestTags).toList(), 2);

        Playlist update = new Playlist();
        update.setId(playlistId);
        update.setSongIds(songIds);
        update.setEmoTags(emoTags);
        update.setInterestTags(interestTags);
        playlistMapper.updateById(update);

        // 收藏数 -1
        Music musicUpdate = new Music();
        musicUpdate.setId(musicId);
        musicMapper.update(
                musicUpdate,
                new LambdaUpdateWrapper<Music>()
                        .eq(Music::getId, musicId)
                        .setSql("collect_count = GREATEST(0, collect_count - 1)")
        );

        // 热度增量：取消当日收藏
        dailyStatsService.cancelCollect("song", Long.valueOf(musicId));

        return syncCollectMusicIds(operatorUserId);
    }

    /**
     * 重新计算用户所有歌单的歌曲并集，同步更新 collect_music_ids
     */
    private List<Integer> syncCollectMusicIds(Integer userId) {
        LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Playlist::getUserId, userId);
        List<Playlist> playlists = playlistMapper.selectList(wrapper);

        LinkedHashSet<Integer> allSongIds = new LinkedHashSet<>();
        for (Playlist pl : playlists) {
            if (pl.getSongIds() != null) {
                allSongIds.addAll(pl.getSongIds());
            }
        }

        List<Integer> collectMusicIds = List.copyOf(allSongIds);
        User update = new User();
        update.setId(userId);
        update.setCollectMusicIds(collectMusicIds);
        userMapper.updateById(update);

        return collectMusicIds;
    }

    @Override
    public void increasePlayCount(Integer id) {
        if (id == null || id <= 0) {
            return;
        }
        Playlist update = new Playlist();
        update.setId(id);
        playlistMapper.update(
                update,
                new LambdaUpdateWrapper<Playlist>()
                        .eq(Playlist::getId, id)
                        .setSql("play_count = play_count + 1")
        );
    }

    /**
     * 校验更新DTO
     */
    private void validateUpdateDTO(PlaylistDTO dto) {
        if (dto == null) {
            throw new BusinessException("歌单信息不能为空");
        }
        if (dto.getId() == null) {
            throw new BusinessException("歌单ID不能为空");
        }
    }
}
