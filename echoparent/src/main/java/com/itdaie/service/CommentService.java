package com.itdaie.service;

import com.itdaie.pojo.dto.CommentDTO;
import com.itdaie.pojo.dto.CommentPageDTO;
import com.itdaie.pojo.vo.PageDataVo;

/**
 * 评论服务接口
 *
 * @author itdaie
 */
public interface CommentService {

    /**
     * 新增评论
     *
     * @param dto 评论数据
     * @throws com.itdaie.common.exception.CommentException 参数校验失败时抛出
     */
    void add(CommentDTO dto);

    /**
     * 修改评论（仅内容）
     *
     * @param dto 评论数据，id字段必填
     * @throws com.itdaie.common.exception.CommentException 评论不存在时抛出
     */
    void update(CommentDTO dto);

    /**
     * 删除评论（逻辑删除）
     *
     * @param id 评论ID
     * @throws com.itdaie.common.exception.CommentException 评论不存在时抛出
     */
    void delete(Integer id);

    /**
     * 分页查询评论
     * 支持多种查询维度，自动利用数据库索引
     *
     * @param dto 查询参数
     * @return 分页结果
     */
    PageDataVo pageQuery(CommentPageDTO dto);

    /**
     * 点赞/取消点赞评论
     *
     * @param commentId 评论ID
     * @param userId    用户ID
     */
    void likeComment(Integer commentId, Integer userId);

    /**
     * 点踩/取消点踩评论
     *
     * @param commentId 评论ID
     * @param userId    用户ID
     */
    void dislikeComment(Integer commentId, Integer userId);
}
