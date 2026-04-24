package com.itdaie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itdaie.common.exception.BusinessException;
import com.itdaie.mapper.SpacePostMapper;
import com.itdaie.mapper.UserMapper;
import com.itdaie.pojo.dto.SpacePostDTO;
import com.itdaie.pojo.dto.SpacePostForwardDTO;
import com.itdaie.pojo.dto.SpacePostPageDTO;
import com.itdaie.pojo.entity.SpacePost;
import com.itdaie.pojo.entity.User;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.SpacePostVO;
import com.itdaie.service.SpacePostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 空间说说服务实现类
 *
 * @author itdaie
 */
@Service
public class SpacePostServiceImpl implements SpacePostService {

    @Autowired
    private SpacePostMapper spacePostMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public void add(SpacePostDTO dto, Integer userId) {
        if (dto == null) {
            throw new BusinessException("说说内容不能为空");
        }
        if (!StringUtils.hasText(dto.getContent())) {
            throw new BusinessException("说说内容不能为空");
        }
        if (dto.getContent().length() > 500) {
            throw new BusinessException("说说内容不能超过500字");
        }
        if (dto.getImages() != null && dto.getImages().size() > 9) {
            throw new BusinessException("图片最多上传9张");
        }

        User user = userMapper.selectById(userId);
        if (user == null || Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new BusinessException("用户不存在");
        }

        SpacePost post = new SpacePost();
        post.setUserId(userId);
        post.setContent(dto.getContent().trim());
        post.setImages(dto.getImages());
        post.setPostType("original");
        post.setExtra(dto.getExtra());
        post.setIsPrivate(dto.getIsPrivate() != null ? dto.getIsPrivate() : false);
        post.setCommentCount(0);
        post.setForwardCount(0);
        post.setDeleted(false);

        spacePostMapper.insert(post);
    }

    @Override
    @Transactional
    public void delete(Long postId, Integer userId) {
        if (postId == null) {
            throw new BusinessException("说说ID不能为空");
        }

        SpacePost post = spacePostMapper.selectById(postId);
        if (post == null || Boolean.TRUE.equals(post.getDeleted())) {
            throw new BusinessException("说说不存在");
        }

        User user = userMapper.selectById(userId);
        boolean isAdmin = user != null && user.getRole() != null && user.getRole() >= 2;
        if (!post.getUserId().equals(userId) && !isAdmin) {
            throw new BusinessException("无权删除该说说");
        }

        SpacePost update = new SpacePost();
        update.setId(postId);
        update.setDeleted(true);
        spacePostMapper.updateById(update);
    }

    @Override
    public PageDataVo pageQuery(SpacePostPageDTO dto) {
        SpacePostPageDTO safeDto = normalizeDto(dto);

        Page<SpacePost> page = new Page<>(safeDto.getPageNum(), safeDto.getPageSize());
        LambdaQueryWrapper<SpacePost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpacePost::getDeleted, false);

        if (safeDto.getUserId() != null) {
            wrapper.eq(SpacePost::getUserId, safeDto.getUserId());
        }

        wrapper.orderByDesc(SpacePost::getCreateTime);

        Page<SpacePost> resultPage = spacePostMapper.selectPage(page, wrapper);
        List<SpacePostVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        return new PageDataVo(resultPage.getTotal(), records);
    }

    @Override
    @Transactional
    public void likePost(Long postId, Integer userId) {
        if (postId == null || userId == null) {
            throw new BusinessException("参数错误");
        }

        SpacePost post = spacePostMapper.selectById(postId);
        if (post == null || Boolean.TRUE.equals(post.getDeleted())) {
            throw new BusinessException("说说不存在");
        }

        LinkedHashSet<Integer> likeIds = post.getLikeIds() != null
                ? new LinkedHashSet<>(post.getLikeIds())
                : new LinkedHashSet<>();

        if (likeIds.contains(userId)) {
            likeIds.remove(userId);
        } else {
            likeIds.add(userId);
        }

        SpacePost update = new SpacePost();
        update.setId(postId);
        update.setLikeIds(new ArrayList<>(likeIds));
        spacePostMapper.updateById(update);
    }

    @Override
    @Transactional
    public void forward(SpacePostForwardDTO dto, Integer userId) {
        if (dto == null || dto.getSourceId() == null) {
            throw new BusinessException("转发来源不能为空");
        }

        SpacePost source = spacePostMapper.selectById(dto.getSourceId());
        if (source == null || Boolean.TRUE.equals(source.getDeleted())) {
            throw new BusinessException("原说说不存在");
        }

        String content = dto.getContent() != null ? dto.getContent().trim() : "";
        if (content.length() > 500) {
            throw new BusinessException("转发文字不能超过500字");
        }

        SpacePost post = new SpacePost();
        post.setUserId(userId);
        post.setContent(content);
        post.setPostType("forward");
        post.setSourceId(source.getId());
        post.setSourceType("space_post");
        post.setSourceUserId(source.getUserId());
        post.setIsPrivate(false);
        post.setCommentCount(0);
        post.setForwardCount(0);
        post.setDeleted(false);

        spacePostMapper.insert(post);

        // 更新原说说转发数
        SpacePost sourceUpdate = new SpacePost();
        sourceUpdate.setId(source.getId());
        sourceUpdate.setForwardCount(source.getForwardCount() + 1);
        spacePostMapper.updateById(sourceUpdate);
    }

    @Override
    public Map<String, Object> getStats(Integer userId) {
        Map<String, Object> stats = new HashMap<>();

        // 说说数
        long postCount = spacePostMapper.selectCount(
                new LambdaQueryWrapper<SpacePost>()
                        .eq(SpacePost::getUserId, userId)
                        .eq(SpacePost::getDeleted, false)
        );
        stats.put("postCount", postCount);

        // 获赞数（累加该用户所有说说的点赞数）
        List<SpacePost> posts = spacePostMapper.selectList(
                new LambdaQueryWrapper<SpacePost>()
                        .eq(SpacePost::getUserId, userId)
                        .eq(SpacePost::getDeleted, false)
        );
        int totalLikes = posts.stream()
                .mapToInt(p -> p.getLikeIds() != null ? p.getLikeIds().size() : 0)
                .sum();
        stats.put("likeCount", totalLikes);

        return stats;
    }

    /**
     * 标准化分页参数
     */
    private SpacePostPageDTO normalizeDto(SpacePostPageDTO dto) {
        if (dto == null) {
            dto = new SpacePostPageDTO();
        }
        if (dto.getPageNum() == null || dto.getPageNum() < 1) {
            dto.setPageNum(1L);
        }
        if (dto.getPageSize() == null || dto.getPageSize() < 1) {
            dto.setPageSize(10L);
        }
        if (dto.getPageSize() > 50) {
            dto.setPageSize(50L);
        }
        return dto;
    }

    /**
     * 转换为VO，补充用户信息
     */
    private SpacePostVO convertToVO(SpacePost post) {
        SpacePostVO vo = new SpacePostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setContent(post.getContent());
        vo.setImages(post.getImages());
        vo.setPostType(post.getPostType());
        vo.setSourceId(post.getSourceId());
        vo.setExtra(post.getExtra());
        vo.setIsPrivate(post.getIsPrivate());
        vo.setLikeIds(post.getLikeIds());
        vo.setLikeCount(post.getLikeIds() != null ? post.getLikeIds().size() : 0);
        vo.setCommentCount(post.getCommentCount());
        vo.setForwardCount(post.getForwardCount());
        vo.setCreateTime(post.getCreateTime());
        vo.setUpdateTime(post.getUpdateTime());

        // 填充发布者信息
        if (post.getUserId() != null) {
            User user = userMapper.selectById(post.getUserId());
            if (user != null) {
                vo.setUserName(user.getName() != null ? user.getName() : user.getUsername());
                vo.setUserAvatar(user.getAvatar());
            }
        }

        // 填充转发来源信息
        if (post.getSourceId() != null && "forward".equals(post.getPostType())) {
            SpacePost source = spacePostMapper.selectById(post.getSourceId());
            if (source != null && !Boolean.TRUE.equals(source.getDeleted())) {
                vo.setSourceContent(source.getContent());
                vo.setSourceImages(source.getImages());
                if (source.getUserId() != null) {
                    User sourceUser = userMapper.selectById(source.getUserId());
                    if (sourceUser != null) {
                        vo.setSourceUserName(sourceUser.getName() != null ? sourceUser.getName() : sourceUser.getUsername());
                        vo.setSourceUserAvatar(sourceUser.getAvatar());
                    }
                }
            }
        }

        return vo;
    }
}
