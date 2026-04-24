package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.common.constants.OssFolder;
import com.itdaie.pojo.dto.SpacePostDTO;
import com.itdaie.pojo.dto.SpacePostForwardDTO;
import com.itdaie.pojo.dto.SpacePostPageDTO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.service.SpacePostService;
import com.itdaie.utils.OssUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 空间说说控制器
 *
 * @author itdaie
 */
@RestController
@RequestMapping("/api/space-posts")
@Slf4j
public class SpacePostController {

    @Autowired
    private SpacePostService spacePostService;

    @Autowired
    private OssUtil ossUtil;

    /**
     * 分页查询空间说说
     */
    @GetMapping("/page")
    public Result<PageDataVo> page(SpacePostPageDTO dto) {
        return Result.success(spacePostService.pageQuery(dto));
    }

    /**
     * 发表说说
     */
    @PostMapping
    public Result<Void> add(@RequestBody SpacePostDTO dto, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        spacePostService.add(dto, userId);
        return Result.success("发表成功", null);
    }

    /**
     * 点赞/取消点赞说说
     */
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        spacePostService.likePost(id, userId);
        return Result.success("操作成功", null);
    }

    /**
     * 转发说说
     */
    @PostMapping("/forward")
    public Result<Void> forward(@RequestBody SpacePostForwardDTO dto, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        spacePostService.forward(dto, userId);
        return Result.success("转发成功", null);
    }

    /**
     * 查询用户空间统计数据
     */
    @GetMapping("/stats")
    public Result<java.util.Map<String, Object>> stats(@RequestParam Integer userId) {
        return Result.success(spacePostService.getStats(userId));
    }

    /**
     * 上传说说图片
     */
    @PostMapping("/upload-image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file,
                                      HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择图片文件");
        }
        String url = ossUtil.upload(file, OssFolder.SPACE_POST_IMAGE);
        return Result.success(url);
    }

    /**
     * 删除说说（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        spacePostService.delete(id, userId);
        return Result.success("删除成功", null);
    }
}
