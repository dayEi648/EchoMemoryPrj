package com.itdaie.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itdaie.common.exception.BusinessException;
import com.itdaie.mapper.AlbumMapper;
import com.itdaie.mapper.MusicMapper;
import com.itdaie.pojo.dto.BannerDTO;
import com.itdaie.pojo.entity.Album;
import com.itdaie.pojo.entity.Music;
import com.itdaie.pojo.vo.BannerVO;
import com.itdaie.service.BannerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 首页轮播推图服务实现。
 * 数据全部存储于 Redis，不依赖数据库表。
 */
@Slf4j
@Service
public class BannerServiceImpl implements BannerService {

    private static final String BANNER_REDIS_KEY = "echomusic:banners";
    private static final int MAX_BANNER_COUNT = 10;
    private static final String TARGET_TYPE_MUSIC = "MUSIC";
    private static final String TARGET_TYPE_ALBUM = "ALBUM";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private AlbumMapper albumMapper;

    @Override
    public List<BannerVO> list() {
        List<BannerItem> items = readFromRedis();
        if (items.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询 DB 补全封面和名称
        List<Integer> musicIds = items.stream()
                .filter(i -> TARGET_TYPE_MUSIC.equals(i.getTargetType()))
                .map(BannerItem::getTargetId)
                .distinct()
                .toList();
        List<Integer> albumIds = items.stream()
                .filter(i -> TARGET_TYPE_ALBUM.equals(i.getTargetType()))
                .map(BannerItem::getTargetId)
                .distinct()
                .toList();

        Map<Integer, Music> musicMap = musicIds.isEmpty() ? Map.of() :
                musicMapper.selectBatchIds(musicIds).stream()
                        .collect(Collectors.toMap(Music::getId, m -> m));
        Map<Integer, Album> albumMap = albumIds.isEmpty() ? Map.of() :
                albumMapper.selectBatchIds(albumIds).stream()
                        .collect(Collectors.toMap(Album::getId, a -> a));

        List<BannerVO> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            BannerItem item = items.get(i);
            BannerVO vo = new BannerVO();
            vo.setId(item.getId());
            vo.setTitle(item.getTitle());
            vo.setDescription(item.getDescription());
            vo.setTargetType(item.getTargetType());
            vo.setTargetId(item.getTargetId());
            vo.setSortOrder(i);

            if (TARGET_TYPE_MUSIC.equals(item.getTargetType())) {
                Music music = musicMap.get(item.getTargetId());
                if (music != null) {
                    vo.setCoverUrl(music.getImage3Url());
                    vo.setTargetName(music.getMusicName());
                }
            } else if (TARGET_TYPE_ALBUM.equals(item.getTargetType())) {
                Album album = albumMap.get(item.getTargetId());
                if (album != null) {
                    vo.setCoverUrl(album.getImage2Url());
                    vo.setTargetName(album.getAlbumName());
                }
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public void add(BannerDTO dto) {
        validateTarget(dto.getTargetType(), dto.getTargetId());

        List<BannerItem> items = readFromRedis();
        if (items.size() >= MAX_BANNER_COUNT) {
            throw new BusinessException("首页轮播推图最多只能有 " + MAX_BANNER_COUNT + " 张");
        }

        BannerItem item = new BannerItem();
        item.setId(UUID.randomUUID().toString().replace("-", ""));
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setTargetType(dto.getTargetType());
        item.setTargetId(dto.getTargetId());

        items.add(item);
        writeToRedis(items);
    }

    @Override
    public void update(String id, BannerDTO dto) {
        validateTarget(dto.getTargetType(), dto.getTargetId());

        List<BannerItem> items = readFromRedis();
        BannerItem target = items.stream()
                .filter(i -> i.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new BusinessException("推图不存在"));

        target.setTitle(dto.getTitle());
        target.setDescription(dto.getDescription());
        target.setTargetType(dto.getTargetType());
        target.setTargetId(dto.getTargetId());

        writeToRedis(items);
    }

    @Override
    public void delete(String id) {
        List<BannerItem> items = readFromRedis();
        boolean removed = items.removeIf(i -> i.getId().equals(id));
        if (!removed) {
            throw new BusinessException("推图不存在");
        }
        writeToRedis(items);
    }

    @Override
    public void reorder(List<String> ids) {
        List<BannerItem> items = readFromRedis();
        Map<String, BannerItem> itemMap = items.stream()
                .collect(Collectors.toMap(BannerItem::getId, i -> i));

        List<BannerItem> reordered = new ArrayList<>();
        for (String id : ids) {
            BannerItem item = itemMap.get(id);
            if (item != null) {
                reordered.add(item);
            }
        }
        writeToRedis(reordered);
    }

    private void validateTarget(String targetType, Integer targetId) {
        if (targetId == null) {
            throw new BusinessException("目标ID不能为空");
        }
        if (TARGET_TYPE_MUSIC.equals(targetType)) {
            Music music = musicMapper.selectById(targetId);
            if (music == null) {
                throw new BusinessException("音乐不存在");
            }
        } else if (TARGET_TYPE_ALBUM.equals(targetType)) {
            Album album = albumMapper.selectById(targetId);
            if (album == null) {
                throw new BusinessException("专辑不存在");
            }
        } else {
            throw new BusinessException("目标类型不正确");
        }
    }

    private List<BannerItem> readFromRedis() {
        String json = stringRedisTemplate.opsForValue().get(BANNER_REDIS_KEY);
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<BannerItem>>() {});
        } catch (Exception e) {
            log.error("读取Redis轮播数据失败", e);
            return new ArrayList<>();
        }
    }

    private void writeToRedis(List<BannerItem> items) {
        try {
            String json = objectMapper.writeValueAsString(items);
            stringRedisTemplate.opsForValue().set(BANNER_REDIS_KEY, json);
        } catch (Exception e) {
            log.error("写入Redis轮播数据失败", e);
            throw new BusinessException("保存失败，请稍后重试");
        }
    }

    /**
     * Redis 中存储的原始轮播条目（内部类）。
     */
    private static class BannerItem {
        private String id;
        private String title;
        private String description;
        private String targetType;
        private Integer targetId;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTargetType() { return targetType; }
        public void setTargetType(String targetType) { this.targetType = targetType; }
        public Integer getTargetId() { return targetId; }
        public void setTargetId(Integer targetId) { this.targetId = targetId; }
    }
}
