package com.itdaie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itdaie.common.Result;
import com.itdaie.common.constants.OssFolder;
import com.itdaie.mapper.PlaylistMapper;
import com.itdaie.pojo.dto.PlaylistDTO;
import com.itdaie.pojo.dto.PlaylistPageDTO;
import com.itdaie.pojo.entity.Playlist;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.PlaylistVO;
import com.itdaie.service.PlaylistService;
import com.itdaie.utils.ImageProcessUtil;
import com.itdaie.utils.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 歌单控制器。
 * 提供歌单的CRUD和分页查询接口。
 */
@RestController
@RequestMapping("/api/playlists")
@Slf4j
public class PlaylistController {

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private PlaylistMapper playlistMapper;

    @Autowired
    private OssUtil ossUtil;

    /**
     * 分页查询歌单。
     * 支持按歌单名称、用户ID、用户昵称、歌曲ID、情绪标签、兴趣标签等条件筛选。
     * 支持按收藏数、播放数、热度等字段排序。
     *
     * @param dto 分页查询条件
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<PageDataVo> page(PlaylistPageDTO dto) {
        return Result.success(playlistService.pageQuery(dto));
    }

    /**
     * 根据ID查询歌单详情。
     *
     * @param id 歌单ID
     * @return 歌单详情
     */
    @GetMapping("/{id}")
    public Result<PlaylistVO> getById(@PathVariable Integer id) {
        PlaylistVO playlistVO = playlistService.getById(id);
        playlistService.increasePlayCount(id);
        playlistVO.setPlayCount(playlistVO.getPlayCount() + 1);
        return Result.success(playlistVO);
    }

    /**
     * 新增歌单。
     *
     * @param dto 歌单信息
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> add(@RequestBody PlaylistDTO dto) {
        playlistService.add(dto);
        return Result.success("新增成功", null);
    }

    /**
     * 修改歌单信息。
     *
     * @param dto 歌单信息（需包含ID）
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> update(@RequestBody PlaylistDTO dto) {
        playlistService.update(dto);
        return Result.success("修改成功", null);
    }

    /**
     * 根据ID删除歌单。
     *
     * @param id 歌单ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        playlistService.deleteById(id);
        return Result.success("删除成功", null);
    }

    /**
     * 批量删除歌单。
     *
     * @param ids 歌单ID列表
     * @return 操作结果
     */
    @DeleteMapping
    public Result<Void> deleteByIds(@RequestParam List<Integer> ids) {
        playlistService.deleteByIds(ids);
        return Result.success("删除成功", null);
    }

    /**
     * 上传歌单封面到 OSS。
     *
     * @param file 封面图片
     * @return 图片访问 URL
     */
    @PostMapping("/cover")
    public Result<String> uploadCover(@RequestParam("file") MultipartFile file) {
        if (!ImageProcessUtil.isImage(file)) {
            return Result.fail("请上传图片文件");
        }
        try {
            InputStream is = ImageProcessUtil.compress(file, 1200, 1200, 0.85f);
            String url = ossUtil.upload(is, OssFolder.PLAYLIST_IMAGE, ".jpg");
            return Result.success(url);
        } catch (IOException e) {
            return Result.fail("图片处理失败: " + e.getMessage());
        }
    }

    /**
     * 首页推荐歌单。
     * 根据当前用户的情绪标签和兴趣标签推荐公开歌单，最多5个；不足时按热度补全。
     *
     * @param request HTTP请求，用于获取当前登录用户ID
     * @return 推荐歌单列表
     */
    @GetMapping("/home-recommend")
    public Result<List<PlaylistVO>> homeRecommend(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        List<PlaylistVO> list = playlistService.homeRecommend(userId);
        return Result.success(list);
    }

    /**
     * 推荐包含指定音乐的歌单。
     * 查询包含该 musicId 的歌单，按热度降序取前3。
     *
     * @param musicId 音乐ID
     * @return 歌单列表
     */
    @GetMapping("/recommend/{musicId}")
    public Result<List<PlaylistVO>> recommendByMusic(@PathVariable Integer musicId) {
        LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("song_ids && {0}::integer[]", (Object) new Integer[]{musicId})
                .orderByDesc(Playlist::getHot)
                .last("LIMIT 3");

        List<Playlist> list = playlistMapper.selectList(wrapper);
        List<PlaylistVO> vos = list.stream().map(this::convertToVO).toList();
        return Result.success(vos);
    }

    /**
     * 添加歌曲到歌单。
     */
    @PostMapping("/{id}/songs/{musicId}")
    public Result<List<Integer>> addSongToPlaylist(@PathVariable Integer id,
                                                    @PathVariable Integer musicId,
                                                    HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        List<Integer> collectMusicIds = playlistService.addSongToPlaylist(id, musicId, userId);
        return Result.success("添加成功", collectMusicIds);
    }

    /**
     * 从歌单移除歌曲。
     */
    @DeleteMapping("/{id}/songs/{musicId}")
    public Result<List<Integer>> removeSongFromPlaylist(@PathVariable Integer id,
                                                         @PathVariable Integer musicId,
                                                         HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        List<Integer> collectMusicIds = playlistService.removeSongFromPlaylist(id, musicId, userId);
        return Result.success("移除成功", collectMusicIds);
    }

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
}
