package com.itdaie.service;

import com.itdaie.pojo.dto.SpacePostDTO;
import com.itdaie.pojo.dto.SpacePostForwardDTO;
import com.itdaie.pojo.dto.SpacePostPageDTO;
import com.itdaie.pojo.vo.PageDataVo;

import java.util.Map;

/**
 * 空间说说服务接口
 *
 * @author itdaie
 */
public interface SpacePostService {

    /**
     * 发表说说
     *
     * @param dto    说说数据
     * @param userId 当前用户ID
     */
    void add(SpacePostDTO dto, Integer userId);

    /**
     * 删除说说（逻辑删除，仅作者或管理员）
     *
     * @param postId 说说ID
     * @param userId 当前用户ID
     */
    void delete(Long postId, Integer userId);

    /**
     * 分页查询某用户的空间说说
     *
     * @param dto 查询参数
     * @return 分页结果
     */
    PageDataVo pageQuery(SpacePostPageDTO dto);

    /**
     * 点赞/取消点赞说说
     *
     * @param postId 说说ID
     * @param userId 当前用户ID
     */
    void likePost(Long postId, Integer userId);

    /**
     * 转发说说到自己的空间
     *
     * @param dto    转发数据
     * @param userId 当前用户ID
     */
    void forward(SpacePostForwardDTO dto, Integer userId);

    /**
     * 查询用户空间统计数据
     *
     * @param userId 用户ID
     * @return 统计信息：postCount, likeCount
     */
    Map<String, Object> getStats(Integer userId);
}
