package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.common.constants.OssFolder;
import com.itdaie.pojo.dto.MusicDTO;
import com.itdaie.pojo.dto.MusicPageDTO;
import com.itdaie.pojo.vo.MusicVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.mapper.MusicMapper;
import com.itdaie.pojo.entity.Music;
import com.itdaie.service.MusicService;
import com.itdaie.utils.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/musics")
@Slf4j
public class MusicController {

    @Autowired
    private MusicService musicService;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private OssUtil ossUtil;

    @GetMapping("/page")
    public Result<PageDataVo> page(MusicPageDTO dto) {
        return Result.success(musicService.pageQuery(dto));
    }

    @GetMapping("/{id}")
    public Result<MusicVO> getById(@PathVariable Integer id) {
        MusicVO musicVO = musicService.getById(id);
        return Result.success(musicVO);
    }

    @PostMapping
    public Result<Void> add(@ModelAttribute MusicDTO dto,
                            @RequestParam(value = "file", required = false) MultipartFile file,
                            @RequestParam(value = "lyricsFile", required = false) MultipartFile lyricsFile,
                            @RequestParam(value = "image1File", required = false) MultipartFile image1File,
                            @RequestParam(value = "image2File", required = false) MultipartFile image2File,
                            @RequestParam(value = "image3File", required = false) MultipartFile image3File) {
        if (file != null && !file.isEmpty()) {
            dto.setFileUrl(ossUtil.upload(file, OssFolder.MUSIC_MP3));
        }
        if (lyricsFile != null && !lyricsFile.isEmpty()) {
            dto.setLyricsUrl(ossUtil.upload(lyricsFile, OssFolder.MUSIC_LYRICS));
        }
        if (image1File != null && !image1File.isEmpty()) {
            dto.setImage1Url(ossUtil.upload(image1File, OssFolder.MUSIC_IMAGE));
        }
        if (image2File != null && !image2File.isEmpty()) {
            dto.setImage2Url(ossUtil.upload(image2File, OssFolder.MUSIC_IMAGE));
        }
        if (image3File != null && !image3File.isEmpty()) {
            dto.setImage3Url(ossUtil.upload(image3File, OssFolder.MUSIC_IMAGE));
        }
        musicService.add(dto);
        return Result.success("新增成功", null);
    }

    @PutMapping
    public Result<Void> update(@ModelAttribute MusicDTO dto,
                               @RequestParam(value = "file", required = false) MultipartFile file,
                               @RequestParam(value = "lyricsFile", required = false) MultipartFile lyricsFile,
                               @RequestParam(value = "image1File", required = false) MultipartFile image1File,
                               @RequestParam(value = "image2File", required = false) MultipartFile image2File,
                               @RequestParam(value = "image3File", required = false) MultipartFile image3File) {
        if (file != null && !file.isEmpty()) {
            dto.setFileUrl(ossUtil.upload(file, OssFolder.MUSIC_MP3));
        }
        if (lyricsFile != null && !lyricsFile.isEmpty()) {
            dto.setLyricsUrl(ossUtil.upload(lyricsFile, OssFolder.MUSIC_LYRICS));
        }
        if (image1File != null && !image1File.isEmpty()) {
            dto.setImage1Url(ossUtil.upload(image1File, OssFolder.MUSIC_IMAGE));
        }
        if (image2File != null && !image2File.isEmpty()) {
            dto.setImage2Url(ossUtil.upload(image2File, OssFolder.MUSIC_IMAGE));
        }
        if (image3File != null && !image3File.isEmpty()) {
            dto.setImage3Url(ossUtil.upload(image3File, OssFolder.MUSIC_IMAGE));
        }
        musicService.update(dto);
        return Result.success("修改成功", null);
    }

    @DeleteMapping
    public Result<Void> delete(@RequestParam List<Integer> ids) {
        musicService.deleteByIds(ids);
        return Result.success("删除成功", null);
    }

    @GetMapping("/search")
    public Result<List<MusicVO>> search(@RequestParam String keyword,
                                        @RequestParam(required = false) Integer limit) {
        return Result.success(musicService.search(keyword, limit));
    }

    /**
     * 首页热门音乐。
     * 取热度最高的6首音乐，结果当天缓存于 Redis。
     *
     * @return 热门音乐列表
     */
    @GetMapping("/home-hot")
    public Result<List<MusicVO>> homeHot() {
        return Result.success(musicService.homeHotMusics());
    }

    /**
     * 首页新歌速递。
     * 取近7天内发布的音乐，按热度降序取前6首；不足6首不补全。
     *
     * @return 新歌列表
     */
    @GetMapping("/home-new")
    public Result<List<MusicVO>> homeNew() {
        return Result.success(musicService.homeNewMusics());
    }

    @GetMapping("/batch")
    public Result<List<MusicVO>> getBatchByIds(@RequestParam List<Integer> ids) {
        return Result.success(musicService.getBatchByIds(ids));
    }

    /**
     * 推荐与指定音乐标签相似的其他音乐。
     * 根据目标音乐的 emoTags 和 interestTags，查询包含任一标签的其他音乐（排除自身），按热度降序取前4。
     *
     * @param id 音乐ID
     * @return 相似音乐列表
     */
    @GetMapping("/recommend/{id}")
    public Result<List<MusicVO>> recommend(@PathVariable Integer id) {
        Music target = musicMapper.selectById(id);
        if (target == null) {
            return Result.success(List.of());
        }

        Set<String> allTags = new HashSet<>();
        if (target.getEmoTags() != null) {
            allTags.addAll(target.getEmoTags());
        }
        if (target.getInterestTags() != null) {
            allTags.addAll(target.getInterestTags());
        }
        if (allTags.isEmpty()) {
            return Result.success(List.of());
        }

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Music> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        String[] tagArray = allTags.toArray(new String[0]);
        wrapper.ne(Music::getId, id)
                .eq(Music::getIsDeleted, false)
                .and(w -> w.apply("emo_tags && {0}::text[]", (Object) tagArray)
                        .or().apply("interest_tags && {0}::text[]", (Object) tagArray))
                .orderByDesc(Music::getHot)
                .last("LIMIT 4");

        List<Music> list = musicMapper.selectList(wrapper);
        List<MusicVO> vos = list.stream().map(this::convertToSimpleVO).toList();
        return Result.success(vos);
    }

    @GetMapping("/{id}/lyrics")
    public Result<String> getLyrics(@PathVariable Integer id) {
        Music music = musicMapper.selectById(id);
        if (music == null || !StringUtils.hasText(music.getLyricsUrl())) {
            return Result.success("");
        }
        try (InputStream is = new URL(music.getLyricsUrl()).openStream();
             Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
            return Result.success(scanner.useDelimiter("\\A").next());
        } catch (Exception e) {
            return Result.success("");
        }
    }

    /**
     * 获取高频标签列表。
     * 从所有未删除音乐中统计指定类型标签的出现频率，返回频率最高的前 limit 个。
     */
    @GetMapping("/top-tags")
    public Result<List<String>> getTopTags(@RequestParam String type,
                                            @RequestParam(required = false) Integer limit) {
        return Result.success(musicService.getTopTags(type, limit));
    }

    private MusicVO convertToSimpleVO(Music music) {
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
}
