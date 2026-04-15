package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.pojo.dto.PlaylistDTO;
import com.itdaie.pojo.dto.PlaylistPageDTO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.PlaylistVO;
import com.itdaie.service.PlaylistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}
