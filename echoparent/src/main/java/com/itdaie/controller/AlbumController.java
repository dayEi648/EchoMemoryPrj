package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.common.constants.OssFolder;
import com.itdaie.pojo.dto.AlbumDTO;
import com.itdaie.pojo.dto.AlbumPageDTO;
import com.itdaie.pojo.vo.AlbumVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.service.AlbumService;
import com.itdaie.utils.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 专辑控制器。
 * 提供专辑的CRUD和分页查询接口。
 */
@RestController
@RequestMapping("/api/albums")
@Slf4j
public class AlbumController {

    @Autowired
    private AlbumService albumService;

    @Autowired
    private OssUtil ossUtil;

    /**
     * 分页查询专辑。
     * 支持按专辑名称、作者ID、作者名称、歌曲ID、情绪标签、兴趣标签等条件筛选。
     * 支持按收藏数、播放数、热度等字段排序。
     *
     * @param dto 分页查询条件
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<PageDataVo> page(AlbumPageDTO dto) {
        return Result.success(albumService.pageQuery(dto));
    }

    /**
     * 根据ID查询专辑详情。
     *
     * @param id 专辑ID
     * @return 专辑详情
     */
    @GetMapping("/{id}")
    public Result<AlbumVO> getById(@PathVariable Integer id) {
        AlbumVO albumVO = albumService.getById(id);
        albumService.increasePlayCount(id);
        albumVO.setPlayCount(albumVO.getPlayCount() + 1);
        return Result.success(albumVO);
    }

    /**
     * 新增专辑。
     *
     * @param dto 专辑信息
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> add(@ModelAttribute AlbumDTO dto,
                            @RequestParam(value = "image1File", required = false) MultipartFile image1File,
                            @RequestParam(value = "image2File", required = false) MultipartFile image2File) {
        if (image1File != null && !image1File.isEmpty()) {
            dto.setImage1Url(ossUtil.upload(image1File, OssFolder.ALBUM_IMAGE));
        }
        if (image2File != null && !image2File.isEmpty()) {
            dto.setImage2Url(ossUtil.upload(image2File, OssFolder.ALBUM_IMAGE));
        }
        albumService.add(dto);
        return Result.success("新增成功", null);
    }

    /**
     * 修改专辑信息。
     *
     * @param dto 专辑信息（需包含ID）
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> update(@ModelAttribute AlbumDTO dto,
                               @RequestParam(value = "image1File", required = false) MultipartFile image1File,
                               @RequestParam(value = "image2File", required = false) MultipartFile image2File) {
        if (image1File != null && !image1File.isEmpty()) {
            dto.setImage1Url(ossUtil.upload(image1File, OssFolder.ALBUM_IMAGE));
        }
        if (image2File != null && !image2File.isEmpty()) {
            dto.setImage2Url(ossUtil.upload(image2File, OssFolder.ALBUM_IMAGE));
        }
        albumService.update(dto);
        return Result.success("修改成功", null);
    }

    /**
     * 批量删除专辑（物理删除），与前端 {@code DELETE /api/albums?ids=1,2} 一致。
     */
    @DeleteMapping
    public Result<Void> deleteByIds(@RequestParam List<Integer> ids) {
        albumService.deleteByIds(ids);
        return Result.success("删除成功", null);
    }

    /**
     * 删除专辑（直接从数据库删除）。
     *
     * @param id 专辑ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        albumService.deletePhysical(id);
        return Result.success("删除成功", null);
    }

    /**
     * 逻辑删除（下架）专辑。
     *
     * @param id 专辑ID
     * @return 操作结果
     */
    @PutMapping("/{id}/delete")
    public Result<Void> deleteLogical(@PathVariable Integer id) {
        albumService.deleteLogical(id);
        return Result.success("下架成功", null);
    }

    /**
     * 恢复（上架）已逻辑删除的专辑。
     *
     * @param id 专辑ID
     * @return 操作结果
     */
    @PutMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Integer id) {
        albumService.restore(id);
        return Result.success("上架成功", null);
    }

    /**
     * 按专辑名称模糊搜索。
     *
     * @param keyword 关键词
     * @param limit   限制条数
     * @return 专辑列表
     */
    @GetMapping("/search")
    public Result<List<AlbumVO>> search(@RequestParam String keyword,
                                         @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return Result.success(albumService.search(keyword, limit));
    }

    /**
     * 首页推荐专辑。
     * 根据当前用户的情绪标签和兴趣标签推荐未删除专辑，最多5个；不足时按热度补全。
     *
     * @param request HTTP请求，用于获取当前登录用户ID
     * @return 推荐专辑列表
     */
    @GetMapping("/home-recommend")
    public Result<List<AlbumVO>> homeRecommend(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return Result.success(albumService.homeRecommend(userId));
    }
}
