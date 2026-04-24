package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.pojo.dto.CommentDTO;
import com.itdaie.pojo.dto.CommentPageDTO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 评论控制器
 *
 * @author itdaie
 */
@RestController
@RequestMapping("/api/comments")
@Slf4j
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 分页查询评论
     * 支持多种查询维度：
     * - 场景查询：sceneType + sceneId
     * - 用户查询：userId
     * - 回复查询：replyCommentId
     * - 嵌套回复查询：nestedReplyCommentId
     * - 精选查询：isRecommended
     * - 排序：like_count/dislike_count/safety/create_time
     */
    @GetMapping("/page")
    public Result<PageDataVo> page(CommentPageDTO dto) {
        return Result.success(commentService.pageQuery(dto));
    }

    /**
     * 新增评论
     */
    @PostMapping
    public Result<Void> add(@RequestBody CommentDTO dto) {
        commentService.add(dto);
        return Result.success("评论成功", null);
    }

    /**
     * 修改评论（仅内容）
     */
    @PutMapping
    public Result<Void> update(@RequestBody CommentDTO dto) {
        commentService.update(dto);
        return Result.success("修改成功", null);
    }

    /**
     * 删除评论（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        commentService.delete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 点赞/取消点赞评论
     */
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Integer id, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        commentService.likeComment(id, userId);
        return Result.success("操作成功", null);
    }

    /**
     * 点踩/取消点踩评论
     */
    @PostMapping("/{id}/dislike")
    public Result<Void> dislike(@PathVariable Integer id, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        commentService.dislikeComment(id, userId);
        return Result.success("操作成功", null);
    }
}
