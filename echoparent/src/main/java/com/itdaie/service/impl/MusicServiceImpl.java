package com.itdaie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itdaie.common.exception.BusinessException;
import com.itdaie.common.util.SortUtils;
import com.itdaie.mapper.MusicMapper;
import com.itdaie.pojo.dto.MusicDTO;
import com.itdaie.pojo.dto.MusicPageDTO;
import com.itdaie.pojo.entity.Music;
import com.itdaie.pojo.vo.MusicVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.service.MusicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MusicServiceImpl implements MusicService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("release_date", "update_time", "create_time", "hot", "play_count", "comment_count", "collect_count");

    private static final Map<String, SFunction<Music, ?>> SORT_FIELD_MAP = buildSortFieldMap();

    // PostgreSQL数组查询谓词模板
    private static final String PRED_EMO_TAGS =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(emo_tags, '{}'::text[])) AS u WHERE u ILIKE CONCAT('%', {0}, '%'))";
    private static final String PRED_INTEREST_TAGS =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(interest_tags, '{}'::text[])) AS u WHERE u ILIKE CONCAT('%', {0}, '%'))";
    private static final String PRED_LANGUAGES =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(languages, '{}'::text[])) AS u WHERE u ILIKE CONCAT('%', {0}, '%'))";
    private static final String PRED_INSTRUMENTS =
            "EXISTS (SELECT 1 FROM unnest(COALESCE(instruments, '{}'::text[])) AS u WHERE u ILIKE CONCAT('%', {0}, '%'))";

    @Autowired
    private MusicMapper musicMapper;

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
                .eq(StringUtils.hasText(safeDto.getStyle()), Music::getStyle, safeDto.getStyle())
                .eq(safeDto.getIsRecommended() != null, Music::getIsRecommended, safeDto.getIsRecommended())
                .eq(safeDto.getIsDeleted() != null, Music::getIsDeleted, safeDto.getIsDeleted());

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
        vo.setIsDeleted(music.getIsDeleted());
        vo.setCreateTime(music.getCreateTime());
        vo.setUpdateTime(music.getUpdateTime());
        return vo;
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
        return convertToVO(music);
    }

    @Override
    @Transactional
    public void add(MusicDTO dto) {
        validateAddDTO(dto);

        Music music = new Music();
        music.setMusicName(dto.getMusicName().trim());
        music.setAuthorIds(dto.getAuthorIds());
        music.setAlbumId(dto.getAlbumId());
        music.setVip(dto.getVip() != null ? dto.getVip() : false);
        music.setSource(dto.getSource());
        music.setEmoTags(dto.getEmoTags());
        music.setInterestTags(dto.getInterestTags());
        music.setStyle(dto.getStyle());
        music.setLanguages(dto.getLanguages());
        music.setInstruments(dto.getInstruments());
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

        Music music = new Music();
        music.setId(id);

        if (StringUtils.hasText(dto.getMusicName())) {
            music.setMusicName(dto.getMusicName().trim());
        }
        if (dto.getAuthorIds() != null) {
            music.setAuthorIds(dto.getAuthorIds());
        }
        if (dto.getAlbumId() != null) {
            music.setAlbumId(dto.getAlbumId());
        }
        if (dto.getVip() != null) {
            music.setVip(dto.getVip());
        }
        if (StringUtils.hasText(dto.getSource())) {
            music.setSource(dto.getSource());
        }
        if (dto.getEmoTags() != null) {
            music.setEmoTags(dto.getEmoTags());
        }
        if (dto.getInterestTags() != null) {
            music.setInterestTags(dto.getInterestTags());
        }
        if (StringUtils.hasText(dto.getStyle())) {
            music.setStyle(dto.getStyle());
        }
        if (dto.getLanguages() != null) {
            music.setLanguages(dto.getLanguages());
        }
        if (dto.getInstruments() != null) {
            music.setInstruments(dto.getInstruments());
        }
        if (dto.getReleaseDate() != null) {
            music.setReleaseDate(dto.getReleaseDate());
        }
        if (dto.getIsRecommended() != null) {
            music.setIsRecommended(dto.getIsRecommended());
        }
        if (dto.getIsDeleted() != null) {
            music.setIsDeleted(dto.getIsDeleted());
        }
        if (StringUtils.hasText(dto.getFileUrl())) {
            music.setFileUrl(dto.getFileUrl());
        }
        if (StringUtils.hasText(dto.getLyricsUrl())) {
            music.setLyricsUrl(dto.getLyricsUrl());
        }
        if (StringUtils.hasText(dto.getImage1Url())) {
            music.setImage1Url(dto.getImage1Url());
        }
        if (StringUtils.hasText(dto.getImage2Url())) {
            music.setImage2Url(dto.getImage2Url());
        }
        if (StringUtils.hasText(dto.getImage3Url())) {
            music.setImage3Url(dto.getImage3Url());
        }

        musicMapper.updateById(music);
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
        musicMapper.deleteByIds(validIds);
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
