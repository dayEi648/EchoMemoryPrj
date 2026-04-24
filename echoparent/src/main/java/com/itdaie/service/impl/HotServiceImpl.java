package com.itdaie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itdaie.common.enums.TargetTypeEnum;
import com.itdaie.mapper.AlbumMapper;
import com.itdaie.mapper.DailyStatsMapper;
import com.itdaie.mapper.HotMapper;
import com.itdaie.mapper.MusicMapper;
import com.itdaie.mapper.PlaylistMapper;
import com.itdaie.pojo.entity.Album;
import com.itdaie.pojo.entity.DailyStats;
import com.itdaie.pojo.entity.Hot;
import com.itdaie.pojo.entity.Music;
import com.itdaie.pojo.entity.Playlist;
import com.itdaie.service.HotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 热度服务实现类
 * 核心逻辑：基于 daily_stats 聚合数据计算热度分数，并同步回原表。
 *
 * @author itdaie
 */
@Slf4j
@Service
public class HotServiceImpl implements HotService {

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private PlaylistMapper playlistMapper;

    @Autowired
    private AlbumMapper albumMapper;

    @Autowired
    private HotMapper hotMapper;

    @Autowired
    private DailyStatsMapper dailyStatsMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 半年前的日期时间，用于筛选活跃作品
     */
    private static final int HALF_YEAR_DAYS = 180;

    /**
     * 分批处理大小，控制每批次查询和更新的数据量
     */
    private static final int BATCH_SIZE = 1000;

    /**
     * 24h 实时热度权重
     */
    private static final double W_24H_PLAY = 5.0;
    private static final double W_24H_COLLECT = 15.0;
    private static final double W_24H_COMMENT = 10.0;

    /**
     * 前6天（7d-24h）热度权重
     */
    private static final double W_6D_PLAY = 1.25;
    private static final double W_6D_COLLECT = 3.75;
    private static final double W_6D_COMMENT = 2.5;

    /**
     * 历史总量权重（开根号后乘的系数）
     */
    private static final double W_TOTAL_PLAY = 0.4;
    private static final double W_TOTAL_COLLECT = 1.2;
    private static final double W_TOTAL_COMMENT = 0.8;

    /**
     * 对数映射系数
     */
    private static final double LOG_COEFFICIENT = 180.0;

    @Override
    @Transactional
    public void batchUpdateHotFromDailyStats(LocalDate yesterday, LocalDate today) {
        log.info("开始批量更新热度，yesterday={}, today={}", yesterday, today);

        try {
            processSongs(yesterday, today);
            processPlaylists(yesterday, today);
            processAlbums(yesterday, today);
        } finally {
            stringRedisTemplate.delete("echomusic:home:hot");
            log.info("已清除首页热门音乐缓存");
        }

        log.info("热度批量更新完成");
    }

    /**
     * 计算热度分数（0~1000）
     */
    private int computeHotScore(int play24h, int collect24h, int comment24h,
                                int play7d, int collect7d, int comment7d,
                                int totalPlay, int totalCollect, int totalComment) {
        // 24h 实时热度
        double score24h = play24h * W_24H_PLAY
                + collect24h * W_24H_COLLECT
                + comment24h * W_24H_COMMENT;

        // 前6天热度（7d总量 - 24h量）
        double score6d = Math.max(0, play7d - play24h) * W_6D_PLAY
                + Math.max(0, collect7d - collect24h) * W_6D_COLLECT
                + Math.max(0, comment7d - comment24h) * W_6D_COMMENT;

        // 历史积淀分（开根号压缩大数）
        double scoreTotal = Math.sqrt(totalPlay) * W_TOTAL_PLAY
                + Math.sqrt(totalCollect) * W_TOTAL_COLLECT
                + Math.sqrt(totalComment) * W_TOTAL_COMMENT;

        double raw = score24h + score6d + scoreTotal;

        // 对数映射到 0~1000
        double hotScore = LOG_COEFFICIENT * Math.log1p(raw / 10.0);
        return (int) Math.round(Math.min(1000.0, Math.max(0.0, hotScore)));
    }

    private void processSongs(LocalDate yesterday, LocalDate today) {
        LocalDateTime halfYearAgo = LocalDateTime.now().minusDays(HALF_YEAR_DAYS);
        LambdaQueryWrapper<Music> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Music::getUpdateTime, halfYearAgo);
        List<Music> allSongs = musicMapper.selectList(wrapper);
        if (allSongs.isEmpty()) {
            return;
        }

        int total = allSongs.size();
        for (int i = 0; i < total; i += BATCH_SIZE) {
            List<Music> batch = allSongs.subList(i, Math.min(i + BATCH_SIZE, total));
            processSongBatch(batch, yesterday, today);
        }

        log.info("歌曲热度更新完成，共 {} 首", total);
    }

    private void processSongBatch(List<Music> songs, LocalDate yesterday, LocalDate today) {
        List<Long> ids = songs.stream().map(m -> Long.valueOf(m.getId())).toList();
        String typeCode = TargetTypeEnum.SONG.getCode();

        Map<Long, DailyStats> yesterdayMap = fetchYesterdayStats(typeCode, ids, yesterday);
        Map<String, Integer> agg7d = fetch7dAgg(typeCode, ids, yesterday);
        Map<Long, Hot> hotMap = fetchExistingHots(typeCode, ids);

        List<Hot> hotList = new ArrayList<>();
        List<Music> musicUpdates = new ArrayList<>();

        for (Music song : songs) {
            Long id = Long.valueOf(song.getId());

            DailyStats yd = yesterdayMap.getOrDefault(id, new DailyStats());
            int play24h = defaultZero(yd.getDailyPlayCount());
            int collect24h = defaultZero(yd.getDailyCollectCount());
            int comment24h = defaultZero(yd.getDailyCommentCount());

            int play7d = agg7d.getOrDefault(key7d(id, "play"), 0);
            int collect7d = agg7d.getOrDefault(key7d(id, "collect"), 0);
            int comment7d = agg7d.getOrDefault(key7d(id, "comment"), 0);

            int totalPlay = defaultZero(song.getPlayCount());
            int totalCollect = defaultZero(song.getCollectCount());
            int totalComment = defaultZero(song.getCommentCount());

            int hotScore = computeHotScore(play24h, collect24h, comment24h,
                    play7d, collect7d, comment7d,
                    totalPlay, totalCollect, totalComment);

            Hot existing = hotMap.get(id);
            int prevScore = existing != null ? defaultZero(existing.getHotScore()) : 0;

            hotList.add(buildHot(typeCode, id, prevScore, play24h, collect24h, comment24h,
                    play7d, collect7d, comment7d, hotScore));

            Music update = new Music();
            update.setId(song.getId());
            update.setHot(hotScore);
            musicUpdates.add(update);
        }

        if (!hotList.isEmpty()) {
            hotMapper.batchUpsert(hotList);
        }
        for (Music m : musicUpdates) {
            musicMapper.updateById(m);
        }
        createTodayRecords(typeCode, ids, today);
    }

    private void processPlaylists(LocalDate yesterday, LocalDate today) {
        LocalDateTime halfYearAgo = LocalDateTime.now().minusDays(HALF_YEAR_DAYS);
        LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Playlist::getUpdateTime, halfYearAgo);
        List<Playlist> allPlaylists = playlistMapper.selectList(wrapper);
        if (allPlaylists.isEmpty()) {
            return;
        }

        int total = allPlaylists.size();
        for (int i = 0; i < total; i += BATCH_SIZE) {
            List<Playlist> batch = allPlaylists.subList(i, Math.min(i + BATCH_SIZE, total));
            processPlaylistBatch(batch, yesterday, today);
        }

        log.info("歌单热度更新完成，共 {} 个", total);
    }

    private void processPlaylistBatch(List<Playlist> playlists, LocalDate yesterday, LocalDate today) {
        List<Long> ids = playlists.stream().map(p -> Long.valueOf(p.getId())).toList();
        String typeCode = TargetTypeEnum.PLAYLIST.getCode();

        Map<Long, DailyStats> yesterdayMap = fetchYesterdayStats(typeCode, ids, yesterday);
        Map<String, Integer> agg7d = fetch7dAgg(typeCode, ids, yesterday);
        Map<Long, Hot> hotMap = fetchExistingHots(typeCode, ids);

        List<Hot> hotList = new ArrayList<>();
        List<Playlist> playlistUpdates = new ArrayList<>();

        for (Playlist playlist : playlists) {
            Long id = Long.valueOf(playlist.getId());

            DailyStats yd = yesterdayMap.getOrDefault(id, new DailyStats());
            int play24h = defaultZero(yd.getDailyPlayCount());
            int collect24h = defaultZero(yd.getDailyCollectCount());
            int comment24h = defaultZero(yd.getDailyCommentCount());

            int play7d = agg7d.getOrDefault(key7d(id, "play"), 0);
            int collect7d = agg7d.getOrDefault(key7d(id, "collect"), 0);
            int comment7d = agg7d.getOrDefault(key7d(id, "comment"), 0);

            int totalPlay = defaultZero(playlist.getPlayCount());
            int totalCollect = defaultZero(playlist.getCollectCount());
            int totalComment = defaultZero(playlist.getCommentCount());

            int hotScore = computeHotScore(play24h, collect24h, comment24h,
                    play7d, collect7d, comment7d,
                    totalPlay, totalCollect, totalComment);

            Hot existing = hotMap.get(id);
            int prevScore = existing != null ? defaultZero(existing.getHotScore()) : 0;

            hotList.add(buildHot(typeCode, id, prevScore, play24h, collect24h, comment24h,
                    play7d, collect7d, comment7d, hotScore));

            Playlist update = new Playlist();
            update.setId(playlist.getId());
            update.setHot(hotScore);
            playlistUpdates.add(update);
        }

        if (!hotList.isEmpty()) {
            hotMapper.batchUpsert(hotList);
        }
        for (Playlist p : playlistUpdates) {
            playlistMapper.updateById(p);
        }
        createTodayRecords(typeCode, ids, today);
    }

    private void processAlbums(LocalDate yesterday, LocalDate today) {
        LocalDateTime halfYearAgo = LocalDateTime.now().minusDays(HALF_YEAR_DAYS);
        LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Album::getUpdateTime, halfYearAgo);
        List<Album> allAlbums = albumMapper.selectList(wrapper);
        if (allAlbums.isEmpty()) {
            return;
        }

        int total = allAlbums.size();
        for (int i = 0; i < total; i += BATCH_SIZE) {
            List<Album> batch = allAlbums.subList(i, Math.min(i + BATCH_SIZE, total));
            processAlbumBatch(batch, yesterday, today);
        }

        log.info("专辑热度更新完成，共 {} 个", total);
    }

    private void processAlbumBatch(List<Album> albums, LocalDate yesterday, LocalDate today) {
        List<Long> ids = albums.stream().map(a -> Long.valueOf(a.getId())).toList();
        String typeCode = TargetTypeEnum.ALBUM.getCode();

        Map<Long, DailyStats> yesterdayMap = fetchYesterdayStats(typeCode, ids, yesterday);
        Map<String, Integer> agg7d = fetch7dAgg(typeCode, ids, yesterday);
        Map<Long, Hot> hotMap = fetchExistingHots(typeCode, ids);

        List<Hot> hotList = new ArrayList<>();
        List<Album> albumUpdates = new ArrayList<>();

        for (Album album : albums) {
            Long id = Long.valueOf(album.getId());

            DailyStats yd = yesterdayMap.getOrDefault(id, new DailyStats());
            int play24h = defaultZero(yd.getDailyPlayCount());
            int collect24h = defaultZero(yd.getDailyCollectCount());
            int comment24h = defaultZero(yd.getDailyCommentCount());

            int play7d = agg7d.getOrDefault(key7d(id, "play"), 0);
            int collect7d = agg7d.getOrDefault(key7d(id, "collect"), 0);
            int comment7d = agg7d.getOrDefault(key7d(id, "comment"), 0);

            int totalPlay = defaultZero(album.getPlayCount());
            int totalCollect = defaultZero(album.getCollectCount());
            // 专辑表无 comment_count 字段
            int totalComment = 0;

            int hotScore = computeHotScore(play24h, collect24h, comment24h,
                    play7d, collect7d, comment7d,
                    totalPlay, totalCollect, totalComment);

            Hot existing = hotMap.get(id);
            int prevScore = existing != null ? defaultZero(existing.getHotScore()) : 0;

            hotList.add(buildHot(typeCode, id, prevScore, play24h, collect24h, comment24h,
                    play7d, collect7d, comment7d, hotScore));

            Album update = new Album();
            update.setId(album.getId());
            update.setHot(hotScore);
            albumUpdates.add(update);
        }

        if (!hotList.isEmpty()) {
            hotMapper.batchUpsert(hotList);
        }
        for (Album a : albumUpdates) {
            albumMapper.updateById(a);
        }
        createTodayRecords(typeCode, ids, today);
    }

    private Map<Long, DailyStats> fetchYesterdayStats(String typeCode, List<Long> ids, LocalDate yesterday) {
        LambdaQueryWrapper<DailyStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyStats::getTargetType, typeCode)
                .in(DailyStats::getTargetId, ids)
                .eq(DailyStats::getStatDate, yesterday);
        return dailyStatsMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(DailyStats::getTargetId, s -> s, (a, b) -> a));
    }

    private Map<String, Integer> fetch7dAgg(String typeCode, List<Long> ids, LocalDate yesterday) {
        LocalDate startDate = yesterday.minusDays(6);
        LambdaQueryWrapper<DailyStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyStats::getTargetType, typeCode)
                .in(DailyStats::getTargetId, ids)
                .ge(DailyStats::getStatDate, startDate)
                .le(DailyStats::getStatDate, yesterday);
        List<DailyStats> list = dailyStatsMapper.selectList(wrapper);

        Map<String, Integer> result = new HashMap<>();
        for (DailyStats ds : list) {
            Long id = ds.getTargetId();
            result.merge(key7d(id, "play"), defaultZero(ds.getDailyPlayCount()), Integer::sum);
            result.merge(key7d(id, "collect"), defaultZero(ds.getDailyCollectCount()), Integer::sum);
            result.merge(key7d(id, "comment"), defaultZero(ds.getDailyCommentCount()), Integer::sum);
        }
        return result;
    }

    private Map<Long, Hot> fetchExistingHots(String typeCode, List<Long> ids) {
        LambdaQueryWrapper<Hot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Hot::getTargetType, typeCode)
                .in(Hot::getTargetId, ids);
        return hotMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(Hot::getTargetId, h -> h, (a, b) -> a));
    }

    private void createTodayRecords(String typeCode, List<Long> ids, LocalDate today) {
        List<DailyStats> records = ids.stream()
                .map(id -> DailyStats.builder()
                        .targetType(typeCode)
                        .targetId(id)
                        .statDate(today)
                        .dailyPlayCount(0)
                        .dailyCollectCount(0)
                        .dailyCommentCount(0)
                        .build())
                .toList();
        if (!records.isEmpty()) {
            dailyStatsMapper.batchInsertTodayRecords(records);
        }
    }

    private Hot buildHot(String typeCode, Long id, int prevScore,
                         int play24h, int collect24h, int comment24h,
                         int play7d, int collect7d, int comment7d,
                         int hotScore) {
        return Hot.builder()
                .targetType(typeCode)
                .targetId(id)
                .prevHotScore(prevScore)
                .playCount24h(play24h)
                .collectCount24h(collect24h)
                .commentCount24h(comment24h)
                .playCount7d(play7d)
                .collectCount7d(collect7d)
                .commentCount7d(comment7d)
                .hotScore(hotScore)
                .build();
    }

    private String key7d(Long id, String metric) {
        return id + ":" + metric;
    }

    private int defaultZero(Integer value) {
        return value != null ? value : 0;
    }
}
