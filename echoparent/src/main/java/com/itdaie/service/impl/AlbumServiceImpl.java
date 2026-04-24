package com.itdaie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itdaie.common.exception.BusinessException;
import com.itdaie.common.util.SortUtils;
import com.itdaie.common.util.TagUtils;
import com.itdaie.mapper.AlbumMapper;
import com.itdaie.mapper.MusicMapper;
import com.itdaie.mapper.UserMapper;
import com.itdaie.pojo.dto.AlbumDTO;
import com.itdaie.pojo.entity.Music;
import com.itdaie.pojo.dto.AlbumPageDTO;
import com.itdaie.pojo.entity.Album;
import com.itdaie.pojo.entity.User;
import com.itdaie.pojo.vo.AlbumVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.service.AlbumService;
import com.itdaie.utils.OssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class AlbumServiceImpl implements AlbumService {

    /**
     * 允许的排序字段白名单
     */
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("collect_count", "play_count", "hot", "create_time", "update_time");

    private static final Map<String, SFunction<Album, ?>> SORT_FIELD_MAP = buildSortFieldMap();

    /**
     * PostgreSQL数组查询谓词模板（模糊匹配）
     */
    private static final String PRED_EMO_TAGS =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(emo_tags, '{}'::text[])) AS u WHERE u ILIKE CONCAT('%', {0}, '%'))";
    private static final String PRED_INTEREST_TAGS =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(interest_tags, '{}'::text[])) AS u WHERE u ILIKE CONCAT('%', {0}, '%'))";
    private static final String PRED_AUTHOR_NAMES =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(author_names, '{}'::text[])) AS u WHERE u ILIKE CONCAT('%', {0}, '%'))";

    /**
     * PostgreSQL数组精确匹配谓词模板（全局搜索）
     */
    private static final String PRED_EMO_TAGS_EXACT =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(emo_tags, '{}'::text[])) AS t WHERE t = {0})";
    private static final String PRED_INTEREST_TAGS_EXACT =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(interest_tags, '{}'::text[])) AS t WHERE t = {0})";

    @Autowired
    private AlbumMapper albumMapper;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private UserMapper userMapper;

    private static final String UNKNOWN_AUTHOR = "未知用户";

    @Override
    public PageDataVo pageQuery(AlbumPageDTO dto) {
        AlbumPageDTO safeDto = normalizeDto(dto);
        String sortBy = safeDto.getSortBy();
        String sortOrder = safeDto.getSortOrder();

        SortUtils.validateSort(sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Page<Album> page = new Page<>(safeDto.getPageNum(), safeDto.getPageSize());
        LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<>();

        // ID精确查询
        wrapper.eq(safeDto.getId() != null, Album::getId, safeDto.getId());

        // 基础等值条件（使用索引：idx_album_recommended, idx_album_active等）
        wrapper.eq(safeDto.getIsRecommended() != null, Album::getIsRecommended, safeDto.getIsRecommended())
                .eq(safeDto.getIsDeleted() != null, Album::getIsDeleted, safeDto.getIsDeleted());

        // 专辑名称模糊查询（使用索引：idx_album_name_trgm）
        wrapper.like(StringUtils.hasText(safeDto.getAlbumName()), Album::getAlbumName, safeDto.getAlbumName());

        // 作者ID数组过滤（使用索引：idx_album_author_ids）
        // 使用 && 操作符检查两个数组是否有交集（包含任一指定作者即可匹配）
        if (!CollectionUtils.isEmpty(safeDto.getAuthorIds())) {
            wrapper.apply("author_ids && {0}::integer[]", (Object) safeDto.getAuthorIds().toArray(new Integer[0]));
        }

        // 歌曲ID数组过滤（使用索引：idx_album_song_ids）
        if (!CollectionUtils.isEmpty(safeDto.getSongIds())) {
            wrapper.apply("song_ids && {0}::integer[]", (Object) safeDto.getSongIds().toArray(new Integer[0]));
        }

        // 作者昵称模糊查询（使用unnest展开数组后模糊匹配）
        if (StringUtils.hasText(safeDto.getAuthorName())) {
            wrapper.apply(PRED_AUTHOR_NAMES, safeDto.getAuthorName().trim());
        }

        // 标签数组模糊查询（使用索引：idx_album_emo_tags / idx_album_interest_tags）
        applyArrayKeywordOrGroup(wrapper, safeDto.getEmoTags(), PRED_EMO_TAGS);
        applyArrayKeywordOrGroup(wrapper, safeDto.getInterestTags(), PRED_INTEREST_TAGS);

        // 排序构建（使用索引：idx_album_hot_desc / idx_album_collect_desc）
        SortUtils.buildSort(sortBy, sortOrder, wrapper, SORT_FIELD_MAP);

        Page<Album> resultPage = albumMapper.selectPage(page, wrapper);
        List<AlbumVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        return new PageDataVo(resultPage.getTotal(), records);
    }

    /**
     * 应用数组关键词OR条件组
     * 多个关键词之间为OR关系：命中任一即满足
     */
    private void applyArrayKeywordOrGroup(LambdaQueryWrapper<Album> wrapper, List<String> keywords,
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
    private AlbumPageDTO normalizeDto(AlbumPageDTO dto) {
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
    private static Map<String, SFunction<Album, ?>> buildSortFieldMap() {
        Map<String, SFunction<Album, ?>> fieldMap = new HashMap<>();
        fieldMap.put("collect_count", Album::getCollectCount);
        fieldMap.put("play_count", Album::getPlayCount);
        fieldMap.put("hot", Album::getHot);
        fieldMap.put("create_time", Album::getCreateTime);
        fieldMap.put("update_time", Album::getUpdateTime);
        return fieldMap;
    }

    /**
     * 实体转换为视图对象
     */
    private AlbumVO convertToVO(Album album) {
        AlbumVO vo = new AlbumVO();
        vo.setId(album.getId());
        vo.setAlbumName(album.getAlbumName());
        vo.setAuthorIds(album.getAuthorIds());
        vo.setAuthorNames(album.getAuthorNames());
        vo.setAlbumDescription(album.getAlbumDescription());
        vo.setSource(album.getSource());
        vo.setEmoTags(album.getEmoTags());
        vo.setInterestTags(album.getInterestTags());
        vo.setCollectCount(album.getCollectCount());
        vo.setPlayCount(album.getPlayCount());
        vo.setHot(album.getHot());
        vo.setImage1Url(album.getImage1Url());
        vo.setImage2Url(album.getImage2Url());
        vo.setSongIds(album.getSongIds());
        vo.setIsRecommended(album.getIsRecommended());
        vo.setIsDeleted(album.getIsDeleted());
        vo.setCreateTime(album.getCreateTime());
        vo.setUpdateTime(album.getUpdateTime());
        return vo;
    }

    @Override
    public AlbumVO getById(Integer id) {
        if (id == null) {
            throw new BusinessException("专辑ID不能为空");
        }
        Album album = albumMapper.selectById(id);
        if (album == null) {
            throw new BusinessException("专辑不存在");
        }
        return convertToVO(album);
    }

    @Override
    @Transactional
    public void add(AlbumDTO dto) {
        validateAddDTO(dto);

        Album album = new Album();
        album.setAlbumName(dto.getAlbumName().trim());
        album.setAlbumDescription(dto.getAlbumDescription());
        album.setSource(dto.getSource());
        album.setImage1Url(dto.getImage1Url());
        album.setImage2Url(dto.getImage2Url());
        album.setIsRecommended(dto.getIsRecommended() != null ? dto.getIsRecommended() : false);
        album.setIsDeleted(false);

        // 初始化计数字段
        album.setCollectCount(0);
        album.setPlayCount(0);
        album.setHot(0);

        albumMapper.insert(album);
        Integer albumId = album.getId();

        // 防御：过滤 songIds 中的 null 和 -1
        List<Integer> songIds = dto.getSongIds() == null ? List.of()
                : dto.getSongIds().stream()
                        .filter(sid -> sid != null && sid != -1)
                        .distinct()
                        .toList();
        syncSongsToAlbum(songIds, albumId);

        this.recomputeAlbumAggregates(albumId, null, null);
    }

    @Override
    @Transactional
    public void update(AlbumDTO dto) {
        validateUpdateDTO(dto);

        Integer id = dto.getId();
        Album existingAlbum = albumMapper.selectById(id);
        if (existingAlbum == null) {
            throw new BusinessException("专辑不存在");
        }

        Album album = new Album();
        album.setId(id);

        if (StringUtils.hasText(dto.getAlbumName())) {
            album.setAlbumName(dto.getAlbumName().trim());
        }
        if (dto.getAlbumDescription() != null) {
            album.setAlbumDescription(dto.getAlbumDescription());
        }
        if (dto.getSource() != null) {
            album.setSource(dto.getSource());
        }
        if (dto.getImage1Url() != null) {
            ossUtil.deleteByPublicUrlIfReplaced(existingAlbum.getImage1Url(), dto.getImage1Url());
            album.setImage1Url(dto.getImage1Url());
        }
        if (dto.getImage2Url() != null) {
            ossUtil.deleteByPublicUrlIfReplaced(existingAlbum.getImage2Url(), dto.getImage2Url());
            album.setImage2Url(dto.getImage2Url());
        }
        if (dto.getIsRecommended() != null) {
            album.setIsRecommended(dto.getIsRecommended());
        }

        albumMapper.updateById(album);

        // 同步 musics.album_id 并处理跨专辑移动
        if (dto.getSongIds() != null) {
            List<Integer> newSongIds = dto.getSongIds().stream()
                    .filter(sid -> sid != null && sid != -1)
                    .distinct()
                    .toList();
            Set<Integer> affectedOldAlbums = syncSongsToAlbum(newSongIds, id);
            for (Integer oldAlbumId : affectedOldAlbums) {
                this.recomputeAlbumAggregates(oldAlbumId);
            }
        }

        this.recomputeAlbumAggregates(id, null, null);
    }

    @Override
    @Transactional
    public void deletePhysical(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException("专辑ID不能为空");
        }
        Album existingAlbum = albumMapper.selectById(id);
        if (existingAlbum == null) {
            throw new BusinessException("专辑不存在");
        }
        ossUtil.deleteByPublicUrls(existingAlbum.getImage1Url(), existingAlbum.getImage2Url());
        albumMapper.deleteById(id);
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
        List<Album> albums = albumMapper.selectBatchIds(validIds);
        for (Album a : albums) {
            ossUtil.deleteByPublicUrls(a.getImage1Url(), a.getImage2Url());
        }
        albumMapper.deleteBatchIds(validIds);
    }

    @Override
    @Transactional
    public void deleteLogical(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException("专辑ID不能为空");
        }
        Album existingAlbum = albumMapper.selectById(id);
        if (existingAlbum == null) {
            throw new BusinessException("专辑不存在");
        }
        Album album = new Album();
        album.setId(id);
        album.setIsDeleted(true);
        albumMapper.updateById(album);
    }

    @Override
    @Transactional
    public void restore(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException("专辑ID不能为空");
        }
        Album existingAlbum = albumMapper.selectById(id);
        if (existingAlbum == null) {
            throw new BusinessException("专辑不存在");
        }
        Album album = new Album();
        album.setId(id);
        album.setIsDeleted(false);
        albumMapper.updateById(album);
    }

    @Override
    public List<AlbumVO> search(String keyword, Integer limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        int safeLimit = (limit == null || limit < 1) ? 10 : Math.min(limit, 100);
        LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Album::getAlbumName, keyword.trim())
                .eq(Album::getIsDeleted, false)
                .orderByDesc(Album::getHot)
                .last("LIMIT " + safeLimit);
        return albumMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public PageDataVo searchAlbums(String keyword, int pageNum, int pageSize) {
        if (!StringUtils.hasText(keyword)) {
            return new PageDataVo(0L, List.of());
        }
        String k = keyword.trim();
        Page<Album> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Album::getIsDeleted, false);

        wrapper.and(w -> {
            w.like(Album::getAlbumName, k)
                    .or().apply(PRED_EMO_TAGS_EXACT, k)
                    .or().apply(PRED_INTEREST_TAGS_EXACT, k);
        });

        wrapper.orderByDesc(Album::getHot);

        Page<Album> resultPage = albumMapper.selectPage(page, wrapper);
        List<AlbumVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        return new PageDataVo(resultPage.getTotal(), records);
    }

    /**
     * 校验新增DTO
     */
    private void validateAddDTO(AlbumDTO dto) {
        if (dto == null) {
            throw new BusinessException("专辑信息不能为空");
        }
        if (!StringUtils.hasText(dto.getAlbumName())) {
            throw new BusinessException("专辑名称不能为空");
        }
    }

    /**
     * 将歌曲列表同步到指定专辑：设置 musics.album_id。
     * 返回被移出歌曲的原 albumId 集合（用于重新计算旧专辑聚合）。
     */
    private Set<Integer> syncSongsToAlbum(List<Integer> songIds, Integer albumId) {
        Set<Integer> affectedOldAlbums = new HashSet<>();
        if (albumId == null || albumId <= 0) {
            return affectedOldAlbums;
        }

        // 查询当前归属该专辑的所有歌曲
        List<Music> currentSongs = musicMapper.selectList(
                new LambdaQueryWrapper<Music>()
                        .eq(Music::getAlbumId, albumId));
        Set<Integer> currentSongIds = currentSongs.stream()
                .map(Music::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> newSongIdSet = new HashSet<>(songIds);

        // 需要移除的：在当前专辑但不在新列表中
        for (Integer sid : currentSongIds) {
            if (!newSongIdSet.contains(sid)) {
                Music m = new Music();
                m.setId(sid);
                m.setAlbumId(null);
                musicMapper.updateById(m);
            }
        }

        // 需要添加的：在新列表中但 album_id 不等于当前专辑
        for (Integer sid : newSongIdSet) {
            Music existing = musicMapper.selectById(sid);
            if (existing == null) {
                continue;
            }
            if (!albumId.equals(existing.getAlbumId())) {
                if (existing.getAlbumId() != null) {
                    affectedOldAlbums.add(existing.getAlbumId());
                }
                Music m = new Music();
                m.setId(sid);
                m.setAlbumId(albumId);
                musicMapper.updateById(m);
            }
        }

        return affectedOldAlbums;
    }

    @Override
    public void recomputeAlbumAggregates(Integer albumId) {
        recomputeAlbumAggregates(albumId, null, null);
    }

    @Override
    public void recomputeAlbumAggregates(Integer albumId, List<String> extraEmoTags, List<String> extraInterestTags) {
        if (albumId == null || albumId <= 0) {
            return;
        }
        Album currentAlbum = albumMapper.selectById(albumId);
        if (currentAlbum == null) {
            return;
        }

        // 查询所有归属该专辑的歌曲，不考虑 is_deleted
        List<Music> musics = musicMapper.selectList(
                new LambdaQueryWrapper<Music>()
                        .eq(Music::getAlbumId, albumId));

        TreeSet<Integer> authorIdSet = new TreeSet<>();

        for (Music m : musics) {
            if (m.getAuthorIds() != null) {
                for (Integer aid : m.getAuthorIds()) {
                    if (aid != null && aid > 0) {
                        authorIdSet.add(aid);
                    }
                }
            }
        }

        List<Integer> sortedAuthorIds = new ArrayList<>(authorIdSet);
        List<String> authorNames = resolveAuthorNames(sortedAuthorIds);

        // 基于歌曲标签统计频率取 top2，并列保留
        List<String> emoTags = TagUtils.computeTopTagsWithTies(
                musics.stream().map(Music::getEmoTags).toList(), 2);
        List<String> interestTags = TagUtils.computeTopTagsWithTies(
                musics.stream().map(Music::getInterestTags).toList(), 2);

        // 合并 extra 标签（去重追加）
        if (extraEmoTags != null && !extraEmoTags.isEmpty()) {
            LinkedHashSet<String> merged = new LinkedHashSet<>(emoTags);
            for (String t : extraEmoTags) {
                if (StringUtils.hasText(t)) {
                    merged.add(t.trim());
                }
            }
            emoTags = new ArrayList<>(merged);
        }
        if (extraInterestTags != null && !extraInterestTags.isEmpty()) {
            LinkedHashSet<String> merged = new LinkedHashSet<>(interestTags);
            for (String t : extraInterestTags) {
                if (StringUtils.hasText(t)) {
                    merged.add(t.trim());
                }
            }
            interestTags = new ArrayList<>(merged);
        }

        Album patch = new Album();
        patch.setId(albumId);
        patch.setAuthorIds(sortedAuthorIds);
        patch.setAuthorNames(authorNames);
        patch.setEmoTags(emoTags);
        patch.setInterestTags(interestTags);
        // albums.song_ids 由 PostgreSQL 触发器在 musics 变更时维护，此处不写

        albumMapper.updateById(patch);
    }

    private List<String> resolveAuthorNames(List<Integer> sortedAuthorIds) {
        if (sortedAuthorIds.isEmpty()) {
            return List.of();
        }
        List<User> users = userMapper.selectBatchIds(sortedAuthorIds);
        Map<Integer, User> byId = new HashMap<>();
        for (User u : users) {
            if (u != null && u.getId() != null) {
                byId.put(u.getId(), u);
            }
        }
        List<String> names = new ArrayList<>(sortedAuthorIds.size());
        for (Integer aid : sortedAuthorIds) {
            names.add(displayName(byId.get(aid)));
        }
        return names;
    }

    private static String displayName(User u) {
        if (u == null) {
            return UNKNOWN_AUTHOR;
        }
        if (StringUtils.hasText(u.getName())) {
            return u.getName().trim();
        }
        if (StringUtils.hasText(u.getUsername())) {
            return u.getUsername().trim();
        }
        return UNKNOWN_AUTHOR;
    }

    @Override
    public List<AlbumVO> homeRecommend(Integer userId) {
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

        List<AlbumVO> result = new ArrayList<>();
        Set<Integer> selectedIds = new LinkedHashSet<>();

        // 标签匹配查询
        if (!allTags.isEmpty()) {
            LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Album::getIsDeleted, false)
                    .and(w -> w.apply("emo_tags && {0}::text[]", (Object) allTags.toArray(new String[0]))
                            .or().apply("interest_tags && {0}::text[]", (Object) allTags.toArray(new String[0])))
                    .orderByDesc(Album::getHot)
                    .last("LIMIT 5");
            List<Album> matched = albumMapper.selectList(wrapper);
            for (Album a : matched) {
                result.add(convertToVO(a));
                selectedIds.add(a.getId());
            }
        }

        // 补全到5个
        if (result.size() < 5) {
            LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Album::getIsDeleted, false);
            if (!selectedIds.isEmpty()) {
                wrapper.notIn(Album::getId, selectedIds);
            }
            wrapper.orderByDesc(Album::getHot)
                    .last("LIMIT " + (5 - result.size()));
            List<Album> fill = albumMapper.selectList(wrapper);
            for (Album a : fill) {
                result.add(convertToVO(a));
            }
        }

        return result;
    }

    @Override
    public void increasePlayCount(Integer id) {
        if (id == null || id <= 0) {
            return;
        }
        Album update = new Album();
        update.setId(id);
        albumMapper.update(
                update,
                new LambdaUpdateWrapper<Album>()
                        .eq(Album::getId, id)
                        .setSql("play_count = play_count + 1")
        );
    }

    /**
     * 校验更新DTO
     */
    private void validateUpdateDTO(AlbumDTO dto) {
        if (dto == null) {
            throw new BusinessException("专辑信息不能为空");
        }
        if (dto.getId() == null) {
            throw new BusinessException("专辑ID不能为空");
        }
    }
}
