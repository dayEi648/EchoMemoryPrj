package com.itdaie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itdaie.common.exception.BusinessException;
import com.itdaie.common.util.SortUtils;
import com.itdaie.mapper.AlbumMapper;
import com.itdaie.mapper.HotMapper;
import com.itdaie.mapper.MusicMapper;
import com.itdaie.mapper.UserMapper;
import com.itdaie.pojo.dto.MusicDTO;
import com.itdaie.pojo.dto.MusicPageDTO;
import com.itdaie.pojo.entity.Album;
import com.itdaie.pojo.entity.Music;
import com.itdaie.pojo.entity.User;
import com.itdaie.pojo.vo.MusicVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.service.AlbumService;
import com.itdaie.service.MusicService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itdaie.utils.FormBindingUtils;
import com.itdaie.utils.OssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class MusicServiceImpl implements MusicService {

    private static final String HOME_HOT_CACHE_KEY = "echomusic:home:hot";

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("release_date", "update_time", "create_time", "hot", "play_count", "comment_count", "collect_count");

    private static final Map<String, SFunction<Music, ?>> SORT_FIELD_MAP = buildSortFieldMap();

    // PostgreSQL数组查询谓词模板（模糊匹配，用于分页查询条件）
    private static final String PRED_EMO_TAGS =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(emo_tags, '{}'::text[])) AS u WHERE u ILIKE CONCAT('%', {0}, '%'))";
    private static final String PRED_INTEREST_TAGS =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(interest_tags, '{}'::text[])) AS u WHERE u ILIKE CONCAT('%', {0}, '%'))";
    private static final String PRED_LANGUAGES =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(languages, '{}'::text[])) AS u WHERE u ILIKE CONCAT('%', {0}, '%'))";
    private static final String PRED_INSTRUMENTS =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(instruments, '{}'::text[])) AS u WHERE u ILIKE CONCAT('%', {0}, '%'))";

    // PostgreSQL数组精确匹配谓词模板（用于全局搜索）
    private static final String PRED_EMO_TAGS_EXACT =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(emo_tags, '{}'::text[])) AS t WHERE t = {0})";
    private static final String PRED_INTEREST_TAGS_EXACT =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(interest_tags, '{}'::text[])) AS t WHERE t = {0})";
    private static final String PRED_LANGUAGES_EXACT =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(languages, '{}'::text[])) AS t WHERE t = {0})";
    private static final String PRED_INSTRUMENTS_EXACT =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(instruments, '{}'::text[])) AS t WHERE t = {0})";

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private AlbumMapper albumMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HotMapper hotMapper;

    @Override
    public PageDataVo pageQuery(MusicPageDTO dto) {
        MusicPageDTO safeDto = normalizeDto(dto);
        String sortBy = safeDto.getSortBy();
        String sortOrder = safeDto.getSortOrder();

        SortUtils.validateSort(sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Page<Music> page = new Page<>(safeDto.getPageNum(), safeDto.getPageSize());
        LambdaQueryWrapper<Music> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasText(safeDto.getMusicName()), Music::getMusicName, safeDto.getMusicName())
                .eq(safeDto.getAlbumId() != null, Music::getAlbumId, safeDto.getAlbumId())
                .eq(safeDto.getVip() != null, Music::getVip, safeDto.getVip())
                .eq(StringUtils.hasText(safeDto.getStyle()), Music::getStyle, safeDto.getStyle());

        // createTimeAfter 筛选（新歌榜）
        if (StringUtils.hasText(safeDto.getCreateTimeAfter())) {
            wrapper.apply("create_time >= {0}::timestamp", safeDto.getCreateTimeAfter());
        }

        // 优化：推荐查询强制加 isDeleted=false，利用 idx_musics_rec_active 部分索引
        if (Boolean.TRUE.equals(safeDto.getIsRecommended())) {
            wrapper.eq(Music::getIsRecommended, true)
                    .eq(Music::getIsDeleted, false);
        } else {
            wrapper.eq(safeDto.getIsRecommended() != null, Music::getIsRecommended, safeDto.getIsRecommended())
                    .eq(safeDto.getIsDeleted() != null, Music::getIsDeleted, safeDto.getIsDeleted());
        }

        // 作者ID数组过滤（使用PostgreSQL数组操作符）
        if (!CollectionUtils.isEmpty(safeDto.getAuthorIds())) {
            // 使用 && 操作符检查两个数组是否有交集（任一匹配）
            // 或者使用 @> 操作符检查是否包含所有指定ID（全部匹配）
            // 这里使用有交集即可匹配
            wrapper.apply("author_ids && {0}::integer[]", (Object) safeDto.getAuthorIds().toArray(new Integer[0]));
        }

        applyArrayKeywordOrGroup(wrapper, safeDto.getEmoTags(), PRED_EMO_TAGS);
        applyArrayKeywordOrGroup(wrapper, safeDto.getInterestTags(), PRED_INTEREST_TAGS);
        applyArrayKeywordOrGroup(wrapper, safeDto.getLanguage(), PRED_LANGUAGES);
        applyArrayKeywordOrGroup(wrapper, safeDto.getInstruments(), PRED_INSTRUMENTS);

        SortUtils.buildSort(sortBy, sortOrder, wrapper, SORT_FIELD_MAP);

        Page<Music> resultPage = musicMapper.selectPage(page, wrapper);

        List<MusicVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        batchEnrichMusicVOs(records, resultPage.getRecords());
        return new PageDataVo(resultPage.getTotal(), records);
    }

    private void applyArrayKeywordOrGroup(LambdaQueryWrapper<Music> wrapper, List<String> keywords,
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

    private MusicPageDTO normalizeDto(MusicPageDTO dto) {
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

    private static Map<String, SFunction<Music, ?>> buildSortFieldMap() {
        Map<String, SFunction<Music, ?>> fieldMap = new HashMap<>();
        fieldMap.put("release_date", Music::getReleaseDate);
        fieldMap.put("update_time", Music::getUpdateTime);
        fieldMap.put("create_time", Music::getCreateTime);
        fieldMap.put("hot", Music::getHot);
        fieldMap.put("play_count", Music::getPlayCount);
        fieldMap.put("comment_count", Music::getCommentCount);
        fieldMap.put("collect_count", Music::getCollectCount);
        return fieldMap;
    }

    private MusicVO convertToVO(Music music) {
        MusicVO vo = new MusicVO();
        vo.setId(music.getId());
        vo.setMusicName(music.getMusicName());
        vo.setAuthorIds(music.getAuthorIds());
        vo.setAlbumId(music.getAlbumId());
        vo.setVip(music.getVip());
        vo.setSource(music.getSource());
        vo.setEmoTags(music.getEmoTags());
        vo.setInterestTags(music.getInterestTags());
        vo.setStyle(music.getStyle());
        vo.setLanguages(music.getLanguages());
        vo.setInstruments(music.getInstruments());
        vo.setCollectCount(music.getCollectCount());
        vo.setHot(music.getHot());
        vo.setCommentCount(music.getCommentCount());
        vo.setPlayCount(music.getPlayCount());
        vo.setIsRecommended(music.getIsRecommended());
        vo.setReleaseDate(music.getReleaseDate());
        vo.setFileUrl(music.getFileUrl());
        vo.setLyricsUrl(music.getLyricsUrl());
        vo.setImage1Url(music.getImage1Url());
        vo.setImage2Url(music.getImage2Url());
        vo.setImage3Url(music.getImage3Url());
        vo.setCreateTime(music.getCreateTime());
        vo.setUpdateTime(music.getUpdateTime());
        return vo;
    }

    /**
     * 批量为 MusicVO 填充 albumName 和 authorNameList。
     * vos 与 sources 必须一一对应。
     */
    private void batchEnrichMusicVOs(List<MusicVO> vos, List<Music> sources) {
        if (vos == null || vos.isEmpty() || sources == null || sources.isEmpty()) {
            return;
        }

        // 收集 albumId 并批量查询 albumName
        Set<Integer> albumIds = new HashSet<>();
        for (Music m : sources) {
            if (m.getAlbumId() != null) {
                albumIds.add(m.getAlbumId());
            }
        }
        Map<Integer, String> albumNameMap = new HashMap<>();
        if (!albumIds.isEmpty()) {
            List<Album> albums = albumMapper.selectBatchIds(albumIds.stream().toList());
            albums.forEach(a -> albumNameMap.put(a.getId(), a.getAlbumName()));
        }

        // 收集 authorIds 并批量查询 authorName
        Set<Integer> allAuthorIds = new HashSet<>();
        for (Music m : sources) {
            if (m.getAuthorIds() != null) {
                allAuthorIds.addAll(m.getAuthorIds());
            }
        }
        Map<Integer, String> authorNameMap = new HashMap<>();
        if (!allAuthorIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(allAuthorIds.stream().toList());
            for (User u : users) {
                if (u != null && u.getId() != null) {
                    authorNameMap.put(u.getId(), displayName(u));
                }
            }
        }

        // 回填
        for (int i = 0; i < vos.size() && i < sources.size(); i++) {
            MusicVO vo = vos.get(i);
            Music source = sources.get(i);
            if (source.getAlbumId() != null) {
                vo.setAlbumName(albumNameMap.get(source.getAlbumId()));
            }
            if (source.getAuthorIds() != null) {
                vo.setAuthorNameList(source.getAuthorIds().stream()
                        .map(authorNameMap::get)
                        .filter(Objects::nonNull)
                        .toList());
            }
        }
    }

    @Override
    public List<MusicVO> search(String keyword, Integer limit) {
        LambdaQueryWrapper<Music> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), Music::getMusicName, keyword)
                .eq(Music::getIsDeleted, false)
                .orderByDesc(Music::getHot);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        List<Music> musics = musicMapper.selectList(wrapper);
        List<MusicVO> vos = musics.stream()
                .map(this::convertToVO)
                .toList();
        batchEnrichMusicVOs(vos, musics);
        return vos;
    }

    @Override
    public List<MusicVO> homeHotMusics() {
        String cachedJson = stringRedisTemplate.opsForValue().get(HOME_HOT_CACHE_KEY);
        if (cachedJson != null && !cachedJson.isBlank()) {
            try {
                return objectMapper.readValue(cachedJson, new TypeReference<List<MusicVO>>() {});
            } catch (Exception e) {
                // 缓存解析失败，继续查数据库
            }
        }

        LambdaQueryWrapper<Music> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Music::getIsDeleted, false)
                .orderByDesc(Music::getHot)
                .last("LIMIT 6");
        List<Music> musics = musicMapper.selectList(wrapper);
        List<MusicVO> vos = musics.stream()
                .map(this::convertToVO)
                .toList();
        batchEnrichMusicVOs(vos, musics);

        // 从 hot 表批量查询 hot_level 和 trend
        List<Integer> musicIds = musics.stream().map(Music::getId).toList();
        if (!musicIds.isEmpty()) {
            List<Map<String, Object>> hotExtras = hotMapper.selectHotExtrasBySongIds(musicIds);
            Map<Integer, Map<String, Object>> hotExtraMap = new HashMap<>();
            for (Map<String, Object> extra : hotExtras) {
                Number targetId = (Number) extra.get("targetId");
                if (targetId != null) {
                    hotExtraMap.put(targetId.intValue(), extra);
                }
            }
            for (MusicVO vo : vos) {
                Map<String, Object> extra = hotExtraMap.get(vo.getId());
                if (extra != null) {
                    Number hotLevel = (Number) extra.get("hotLevel");
                    vo.setHotLevel(hotLevel != null ? hotLevel.intValue() : null);
                    vo.setTrend((String) extra.get("trend"));
                }
            }
        }

        long secondsUntilMidnight = LocalDateTime.now().until(
                LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay(),
                ChronoUnit.SECONDS);
        try {
            stringRedisTemplate.opsForValue().set(HOME_HOT_CACHE_KEY, objectMapper.writeValueAsString(vos),
                    secondsUntilMidnight, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 缓存写入失败不影响返回结果
        }

        return vos;
    }

    @Override
    public List<MusicVO> homeNewMusics() {
        LambdaQueryWrapper<Music> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Music::getIsDeleted, false)
                .apply("create_time >= NOW() - INTERVAL '7 days'")
                .orderByDesc(Music::getHot)
                .last("LIMIT 6");
        List<Music> musics = musicMapper.selectList(wrapper);
        List<MusicVO> vos = musics.stream()
                .map(this::convertToVO)
                .toList();
        batchEnrichMusicVOs(vos, musics);
        return vos;
    }

    @Override
    public PageDataVo searchMusics(String keyword, int pageNum, int pageSize) {
        if (!StringUtils.hasText(keyword)) {
            return new PageDataVo(0L, List.of());
        }
        String k = keyword.trim();
        Page<Music> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Music> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Music::getIsDeleted, false);

        String authorSongSubquery =
                "id IN (SELECT DISTINCT unnest(song_ids) FROM users WHERE is_deleted = false AND (name ILIKE {0} OR username ILIKE {0}))";
        String albumSongSubquery =
                "id IN (SELECT DISTINCT unnest(song_ids) FROM albums WHERE is_deleted = false AND album_name ILIKE {0})";

        wrapper.and(w -> {
            w.like(Music::getMusicName, k)
                    .or().apply(authorSongSubquery, "%" + k + "%")
                    .or().apply(albumSongSubquery, "%" + k + "%")
                    .or().eq(Music::getStyle, k)
                    .or().apply(PRED_EMO_TAGS_EXACT, k)
                    .or().apply(PRED_INTEREST_TAGS_EXACT, k)
                    .or().apply(PRED_LANGUAGES_EXACT, k)
                    .or().apply(PRED_INSTRUMENTS_EXACT, k);
        });

        // 按匹配字段优先级排序：歌名(0) > 作者(1) > 专辑(2) > 其他(3)，同梯队按热度降序
        String esc = k.replace("'", "''");
        wrapper.last("ORDER BY CASE " +
            "WHEN music_name ILIKE '%" + esc + "%' THEN 0 " +
            "WHEN id IN (SELECT DISTINCT unnest(song_ids) FROM users WHERE is_deleted = false AND (name ILIKE '%" + esc + "%' OR username ILIKE '%" + esc + "%')) THEN 1 " +
            "WHEN id IN (SELECT DISTINCT unnest(song_ids) FROM albums WHERE is_deleted = false AND album_name ILIKE '%" + esc + "%') THEN 2 " +
            "ELSE 3 END, hot DESC");

        Page<Music> resultPage = musicMapper.selectPage(page, wrapper);

        List<MusicVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        batchEnrichMusicVOs(records, resultPage.getRecords());
        return new PageDataVo(resultPage.getTotal(), records);
    }

    private static String displayName(User u) {
        if (u == null) {
            return "未知用户";
        }
        if (StringUtils.hasText(u.getName())) {
            return u.getName().trim();
        }
        if (StringUtils.hasText(u.getUsername())) {
            return u.getUsername().trim();
        }
        return "未知用户";
    }

    @Override
    public List<MusicVO> getBatchByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Integer> validIds = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (validIds.isEmpty()) {
            return List.of();
        }
        List<Music> musics = musicMapper.selectBatchIds(validIds);
        List<MusicVO> vos = musics.stream()
                .map(this::convertToVO)
                .toList();
        batchEnrichMusicVOs(vos, musics);
        return vos;
    }

    @Override
    public MusicVO getById(Integer id) {
        if (id == null) {
            throw new BusinessException("音乐ID不能为空");
        }
        Music music = musicMapper.selectById(id);
        if (music == null) {
            throw new BusinessException("音乐不存在");
        }
        MusicVO vo = convertToVO(music);
        batchEnrichMusicVOs(List.of(vo), List.of(music));
        return vo;
    }

    @Override
    @Transactional
    public void add(MusicDTO dto) {
        validateAddDTO(dto);

        Music music = new Music();
        music.setMusicName(dto.getMusicName().trim());
        music.setAuthorIds(FormBindingUtils.normalizeIntegerList(dto.getAuthorIds()));
        music.setAlbumId(dto.getAlbumId());
        music.setVip(dto.getVip() != null ? dto.getVip() : false);
        music.setSource(dto.getSource());
        if (dto.getEmoTags() != null) {
            music.setEmoTags(FormBindingUtils.normalizeStringList(dto.getEmoTags()));
        }
        if (dto.getInterestTags() != null) {
            music.setInterestTags(FormBindingUtils.normalizeStringList(dto.getInterestTags()));
        }
        music.setStyle(dto.getStyle());
        if (dto.getLanguages() != null) {
            music.setLanguages(FormBindingUtils.normalizeStringList(dto.getLanguages()));
        }
        if (dto.getInstruments() != null) {
            music.setInstruments(FormBindingUtils.normalizeStringList(dto.getInstruments()));
        }
        music.setReleaseDate(dto.getReleaseDate());
        music.setIsRecommended(dto.getIsRecommended() != null ? dto.getIsRecommended() : false);
        music.setIsDeleted(dto.getIsDeleted() != null ? dto.getIsDeleted() : false);
        music.setFileUrl(dto.getFileUrl());
        music.setLyricsUrl(dto.getLyricsUrl());
        music.setImage1Url(dto.getImage1Url());
        music.setImage2Url(dto.getImage2Url());
        music.setImage3Url(dto.getImage3Url());

        music.setCollectCount(0);
        music.setCommentCount(0);
        music.setPlayCount(0);
        music.setHot(0);

        musicMapper.insert(music);
        if (music.getAlbumId() != null) {
            albumService.recomputeAlbumAggregates(music.getAlbumId());
        }
        stringRedisTemplate.delete(HOME_HOT_CACHE_KEY);
    }

    @Override
    @Transactional
    public void update(MusicDTO dto) {
        validateUpdateDTO(dto);

        Integer id = dto.getId();
        Music existingMusic = musicMapper.selectById(id);
        if (existingMusic == null) {
            throw new BusinessException("音乐不存在");
        }
        Integer oldAlbumId = existingMusic.getAlbumId();
        Music music = new Music();
        music.setId(id);

        if (StringUtils.hasText(dto.getMusicName())) {
            music.setMusicName(dto.getMusicName().trim());
        }
        if (dto.getAuthorIds() != null) {
            music.setAuthorIds(FormBindingUtils.normalizeIntegerList(dto.getAuthorIds()));
        }
        if (dto.getAlbumId() != null) {
            music.setAlbumId(dto.getAlbumId() == -1 ? null : dto.getAlbumId());
        }
        if (dto.getVip() != null) {
            music.setVip(dto.getVip());
        }
        if (dto.getSource() != null) {
            music.setSource(dto.getSource());
        }
        if (dto.getEmoTags() != null) {
            music.setEmoTags(FormBindingUtils.normalizeStringList(dto.getEmoTags()));
        }
        if (dto.getInterestTags() != null) {
            music.setInterestTags(FormBindingUtils.normalizeStringList(dto.getInterestTags()));
        }
        if (dto.getStyle() != null) {
            music.setStyle(dto.getStyle());
        }
        if (dto.getLanguages() != null) {
            music.setLanguages(FormBindingUtils.normalizeStringList(dto.getLanguages()));
        }
        if (dto.getInstruments() != null) {
            music.setInstruments(FormBindingUtils.normalizeStringList(dto.getInstruments()));
        }
        if (dto.getReleaseDate() != null) {
            music.setReleaseDate(dto.getReleaseDate());
        }
        if (dto.getHot() != null) {
            music.setHot(dto.getHot());
        }
        if (dto.getIsRecommended() != null) {
            music.setIsRecommended(dto.getIsRecommended());
        }
        if (dto.getIsDeleted() != null) {
            music.setIsDeleted(dto.getIsDeleted());
        }
        if (StringUtils.hasText(dto.getFileUrl())) {
            ossUtil.deleteByPublicUrlIfReplaced(existingMusic.getFileUrl(), dto.getFileUrl());
            music.setFileUrl(dto.getFileUrl());
        }
        if (StringUtils.hasText(dto.getLyricsUrl())) {
            ossUtil.deleteByPublicUrlIfReplaced(existingMusic.getLyricsUrl(), dto.getLyricsUrl());
            music.setLyricsUrl(dto.getLyricsUrl());
        }
        if (StringUtils.hasText(dto.getImage1Url())) {
            ossUtil.deleteByPublicUrlIfReplaced(existingMusic.getImage1Url(), dto.getImage1Url());
            music.setImage1Url(dto.getImage1Url());
        }
        if (StringUtils.hasText(dto.getImage2Url())) {
            ossUtil.deleteByPublicUrlIfReplaced(existingMusic.getImage2Url(), dto.getImage2Url());
            music.setImage2Url(dto.getImage2Url());
        }
        if (StringUtils.hasText(dto.getImage3Url())) {
            ossUtil.deleteByPublicUrlIfReplaced(existingMusic.getImage3Url(), dto.getImage3Url());
            music.setImage3Url(dto.getImage3Url());
        }

        musicMapper.updateById(music);

        Music after = musicMapper.selectById(id);
        Integer newAlbumId = after != null ? after.getAlbumId() : null;

        Set<Integer> affectedAlbums = new HashSet<>();
        if (oldAlbumId != null) {
            affectedAlbums.add(oldAlbumId);
        }
        if (newAlbumId != null) {
            affectedAlbums.add(newAlbumId);
        }
        for (Integer aid : affectedAlbums) {
            albumService.recomputeAlbumAggregates(aid);
        }
        stringRedisTemplate.delete(HOME_HOT_CACHE_KEY);
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
        List<Music> musics = musicMapper.selectBatchIds(validIds);
        Set<Integer> affectedAlbums = new HashSet<>();
        for (Music m : musics) {
            if (m.getAlbumId() != null) {
                affectedAlbums.add(m.getAlbumId());
            }
            ossUtil.deleteByPublicUrls(
                    m.getFileUrl(), m.getLyricsUrl(), m.getImage1Url(), m.getImage2Url(), m.getImage3Url());
        }
        musicMapper.deleteByIds(validIds);
        for (Integer aid : affectedAlbums) {
            albumService.recomputeAlbumAggregates(aid);
        }
        stringRedisTemplate.delete(HOME_HOT_CACHE_KEY);
    }

    @Override
    public List<String> getTopTags(String type, Integer limit) {
        if (limit == null || limit < 1) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }

        String cacheKey = "echomusic:tags:" + type;
        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null && !cachedJson.isBlank()) {
            try {
                List<String> cached = objectMapper.readValue(cachedJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                return cached.stream().limit(limit).toList();
            } catch (Exception e) {
                // 缓存解析失败，继续查数据库
            }
        }

        LambdaQueryWrapper<Music> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Music::getIsDeleted, false);
        List<Music> musics = musicMapper.selectList(wrapper);

        Map<String, Integer> freqMap = new HashMap<>();
        for (Music m : musics) {
            switch (type) {
                case "emotion" -> addTagsToFreqMap(m.getEmoTags(), freqMap);
                case "interest" -> addTagsToFreqMap(m.getInterestTags(), freqMap);
                case "style" -> addSingleTagToFreqMap(m.getStyle(), freqMap);
                case "instrument" -> addTagsToFreqMap(m.getInstruments(), freqMap);
                case "language" -> addTagsToFreqMap(m.getLanguages(), freqMap);
                default -> {
                    // 未知类型，返回空列表
                }
            }
        }

        List<String> result = freqMap.entrySet().stream()
                .filter(e -> StringUtils.hasText(e.getKey()))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result),
                    1, java.util.concurrent.TimeUnit.HOURS);
        } catch (Exception e) {
            // 缓存写入失败不影响返回结果
        }

        return result;
    }

    private void addTagsToFreqMap(List<String> tags, Map<String, Integer> freqMap) {
        if (tags == null) {
            return;
        }
        for (String tag : tags) {
            if (StringUtils.hasText(tag)) {
                String key = tag.trim();
                freqMap.merge(key, 1, Integer::sum);
            }
        }
    }

    private void addSingleTagToFreqMap(String tag, Map<String, Integer> freqMap) {
        if (StringUtils.hasText(tag)) {
            String key = tag.trim();
            freqMap.merge(key, 1, Integer::sum);
        }
    }

    private void validateAddDTO(MusicDTO dto) {
        if (dto == null) {
            throw new BusinessException("音乐信息不能为空");
        }
        if (!StringUtils.hasText(dto.getMusicName())) {
            throw new BusinessException("音乐名称不能为空");
        }
    }

    private void validateUpdateDTO(MusicDTO dto) {
        if (dto == null) {
            throw new BusinessException("音乐信息不能为空");
        }
        if (dto.getId() == null) {
            throw new BusinessException("音乐ID不能为空");
        }
    }
}
