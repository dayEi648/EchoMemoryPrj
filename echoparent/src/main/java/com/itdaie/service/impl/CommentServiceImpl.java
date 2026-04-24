package com.itdaie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itdaie.common.enums.CommentSceneEnum;
import com.itdaie.common.exception.BusinessException;
import com.itdaie.common.util.SortUtils;
import com.itdaie.mapper.AlbumMapper;
import com.itdaie.mapper.CommentMapper;
import com.itdaie.mapper.MusicMapper;
import com.itdaie.mapper.PlaylistMapper;
import com.itdaie.mapper.SpacePostMapper;
import com.itdaie.mapper.UserMapper;
import com.itdaie.pojo.dto.CommentDTO;
import com.itdaie.pojo.dto.CommentPageDTO;
import com.itdaie.pojo.entity.Album;
import com.itdaie.pojo.entity.Comment;
import com.itdaie.pojo.entity.Music;
import com.itdaie.pojo.entity.Playlist;
import com.itdaie.pojo.entity.SpacePost;
import com.itdaie.pojo.entity.User;
import com.itdaie.pojo.vo.CommentVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.service.CommentService;
import com.itdaie.service.DailyStatsService;
import com.itdaie.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 评论服务实现类
 *
 * @author itdaie
 */
@Service
public class CommentServiceImpl implements CommentService {

    /**
     * 允许的排序字段白名单
     * 对应数据库索引：idx_comments_like_count/dislike_count/safety/create_time
     */
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("like_count", "dislike_count", "safety", "create_time");

    private static final Map<String, SFunction<Comment, ?>> SORT_FIELD_MAP = buildSortFieldMap();

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private PlaylistMapper playlistMapper;

    @Autowired
    private AlbumMapper albumMapper;

    @Autowired
    private SpacePostMapper spacePostMapper;

    @Autowired
    private DailyStatsService dailyStatsService;

    /**
     * 构建排序字段映射
     */
    private static Map<String, SFunction<Comment, ?>> buildSortFieldMap() {
        Map<String, SFunction<Comment, ?>> fieldMap = new HashMap<>();
        fieldMap.put("like_count", Comment::getLikeCount);
        fieldMap.put("dislike_count", Comment::getDislikeCount);
        fieldMap.put("safety", Comment::getSafety);
        fieldMap.put("create_time", Comment::getCreateTime);
        return fieldMap;
    }

    @Override
    @Transactional
    public void add(CommentDTO dto) {
        validateAddDTO(dto);

        Comment comment = buildCommentFromDTO(dto);

        // 初始化统计字段（like_count/dislike_count 为 GENERATED ALWAYS，由数据库自动维护）
        comment.setAnswerCount(0);
        comment.setSafety(10);
        comment.setIsRecommended(false);
        comment.setIsDeleted(false);

        commentMapper.insert(comment);

        // 如果是回复，更新被回复评论的answer_count
        updateAnswerCount(comment);

        // 更新作品评论数及热度增量
        updateTargetCommentCount(comment);

        // 发送评论相关通知
        sendCommentNotifications(comment, dto);
    }

    @Override
    @Transactional
    public void update(CommentDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("评论ID不能为空");
        }

        Comment existing = commentMapper.selectById(dto.getId());
        if (existing == null || Boolean.TRUE.equals(existing.getIsDeleted())) {
            throw new BusinessException("评论不存在");
        }

        // 只能修改内容
        Comment comment = new Comment();
        comment.setId(dto.getId());
        comment.setContent(dto.getContent());

        commentMapper.updateById(comment);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (id == null) {
            throw new BusinessException("评论ID不能为空");
        }

        Comment existing = commentMapper.selectById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getIsDeleted())) {
            throw new BusinessException("评论不存在");
        }

        // 逻辑删除
        Comment comment = new Comment();
        comment.setId(id);
        comment.setIsDeleted(true);
        commentMapper.updateById(comment);

        // 回退目标作品评论数
        rollbackTargetCommentCount(existing);

        // 回退父评论回复数
        rollbackAnswerCount(existing);
    }

    @Override
    @Transactional
    public void likeComment(Integer commentId, Integer userId) {
        if (commentId == null || userId == null) {
            throw new BusinessException("参数错误");
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || Boolean.TRUE.equals(comment.getIsDeleted())) {
            throw new BusinessException("评论不存在");
        }

        java.util.LinkedHashSet<Integer> likeIds = comment.getLikeIds() != null
                ? new java.util.LinkedHashSet<>(comment.getLikeIds())
                : new java.util.LinkedHashSet<>();
        java.util.LinkedHashSet<Integer> dislikeIds = comment.getDislikeIds() != null
                ? new java.util.LinkedHashSet<>(comment.getDislikeIds())
                : new java.util.LinkedHashSet<>();

        boolean isNewLike = !likeIds.contains(userId);
        if (likeIds.contains(userId)) {
            // 已点赞 → 取消点赞
            likeIds.remove(userId);
        } else {
            // 未点赞 → 添加点赞，同时移除点踩
            likeIds.add(userId);
            dislikeIds.remove(userId);
        }

        comment.setLikeIds(new java.util.ArrayList<>(likeIds));
        comment.setDislikeIds(new java.util.ArrayList<>(dislikeIds));
        commentMapper.updateById(comment);

        // 新增点赞时通知评论作者
        if (isNewLike && comment.getUserId() != null && !comment.getUserId().equals(userId)) {
            notificationService.sendNotification(
                    comment.getUserId(), "like", userId,
                    "comment", Long.valueOf(commentId),
                    "评论点赞", "有人赞了你的评论");
        }
    }

    @Override
    @Transactional
    public void dislikeComment(Integer commentId, Integer userId) {
        if (commentId == null || userId == null) {
            throw new BusinessException("参数错误");
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || Boolean.TRUE.equals(comment.getIsDeleted())) {
            throw new BusinessException("评论不存在");
        }

        java.util.LinkedHashSet<Integer> likeIds = comment.getLikeIds() != null
                ? new java.util.LinkedHashSet<>(comment.getLikeIds())
                : new java.util.LinkedHashSet<>();
        java.util.LinkedHashSet<Integer> dislikeIds = comment.getDislikeIds() != null
                ? new java.util.LinkedHashSet<>(comment.getDislikeIds())
                : new java.util.LinkedHashSet<>();

        if (dislikeIds.contains(userId)) {
            // 已点踩 → 取消点踩
            dislikeIds.remove(userId);
        } else {
            // 未点踩 → 添加点踩，同时移除点赞
            dislikeIds.add(userId);
            likeIds.remove(userId);
        }

        comment.setLikeIds(new java.util.ArrayList<>(likeIds));
        comment.setDislikeIds(new java.util.ArrayList<>(dislikeIds));
        commentMapper.updateById(comment);
    }

    @Override
    public PageDataVo pageQuery(CommentPageDTO dto) {
        CommentPageDTO safeDto = normalizeDto(dto);
        String sortBy = safeDto.getSortBy();
        String sortOrder = safeDto.getSortOrder();

        SortUtils.validateSort(sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Page<Comment> page = new Page<>(safeDto.getPageNum(), safeDto.getPageSize());
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();

        // 基础条件：未删除
        wrapper.eq(Comment::getIsDeleted, false);

        // 根据查询维度构建条件（利用不同索引）
        buildQueryConditions(wrapper, safeDto);

        // 排序
        SortUtils.buildSort(sortBy, sortOrder, wrapper, SORT_FIELD_MAP);

        Page<Comment> resultPage = commentMapper.selectPage(page, wrapper);
        List<CommentVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        return new PageDataVo(resultPage.getTotal(), records);
    }

    /**
     * 构建查询条件
     * 根据传入的参数自动选择最优索引路径
     */
    private void buildQueryConditions(LambdaQueryWrapper<Comment> wrapper, CommentPageDTO dto) {
        // 1. 场景查询（利用条件索引）
        if (StringUtils.hasText(dto.getSceneType()) && dto.getSceneId() != null) {
            CommentSceneEnum scene = CommentSceneEnum.fromCode(dto.getSceneType());
            if (scene != null) {
                // 设置场景标记和ID，确保走条件索引
                switch (scene) {
                    case MUSIC -> wrapper.eq(Comment::getInMusic, true)
                            .eq(Comment::getMusicId, dto.getSceneId());
                    case PLAYLIST -> wrapper.eq(Comment::getInPlaylist, true)
                            .eq(Comment::getPlaylistId, dto.getSceneId());
                    case SPACE -> wrapper.eq(Comment::getInSpace, true)
                            .eq(Comment::getSpaceId, dto.getSceneId());
                }
            }
        }

        // 2. 用户ID查询（利用索引：idx_comments_user_id）
        if (dto.getUserId() != null) {
            wrapper.eq(Comment::getUserId, dto.getUserId());
        }

        // 3. 回复查询（利用条件索引：idx_comments_reply_comment_id）
        if (dto.getReplyCommentId() != null) {
            wrapper.eq(Comment::getIsReply, true)
                    .eq(Comment::getReplyCommentId, dto.getReplyCommentId());
        }

        // 4. 嵌套回复查询（利用条件索引：idx_comments_nested_reply_comment_id）
        if (dto.getNestedReplyCommentId() != null) {
            wrapper.eq(Comment::getIsNestedReply, true)
                    .eq(Comment::getNestedReplyCommentId, dto.getNestedReplyCommentId());
        }

        // 5. 是否回复过滤（用于区分主评论和回复列表）
        if (dto.getIsReply() != null) {
            wrapper.eq(Comment::getIsReply, dto.getIsReply());
        }

        // 6. 精选评论查询
        if (dto.getIsRecommended() != null) {
            wrapper.eq(Comment::getIsRecommended, dto.getIsRecommended());
        }
    }

    /**
     * 校验新增DTO
     */
    private void validateAddDTO(CommentDTO dto) {
        if (dto == null) {
            throw new BusinessException("评论数据不能为空");
        }

        // 场景校验（4选1约束）
        if (!StringUtils.hasText(dto.getSceneType())) {
            throw new BusinessException("评论场景不能为空");
        }
        CommentSceneEnum scene = CommentSceneEnum.fromCode(dto.getSceneType());
        if (scene == null) {
            throw new BusinessException("无效的评论场景，可选：music/playlist/space");
        }
        if (dto.getSceneId() == null) {
            throw new BusinessException("场景对象ID不能为空");
        }

        // 用户校验
        if (dto.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }

        // 内容校验
        if (!StringUtils.hasText(dto.getContent())) {
            throw new BusinessException("评论内容不能为空");
        }
        if (dto.getContent().length() > 2000) {
            throw new BusinessException("评论内容不能超过2000字符");
        }

        // 回复校验
        if (Boolean.TRUE.equals(dto.getIsReply())) {
            if (dto.getReplyCommentId() == null) {
                throw new BusinessException("回复评论ID不能为空");
            }
            // 验证被回复评论是否存在
            Comment replyTo = commentMapper.selectById(dto.getReplyCommentId());
            if (replyTo == null || Boolean.TRUE.equals(replyTo.getIsDeleted())) {
                throw new BusinessException("被回复的评论不存在");
            }
        }

        // 嵌套回复校验
        if (Boolean.TRUE.equals(dto.getIsNestedReply())) {
            if (dto.getNestedReplyCommentId() == null) {
                throw new BusinessException("嵌套回复ID不能为空");
            }
            if (dto.getReplyCommentId() == null) {
                throw new BusinessException("嵌套回复必须指定父级回复");
            }
            // 验证被嵌套回复的评论是否存在
            Comment nestedReplyTo = commentMapper.selectById(dto.getNestedReplyCommentId());
            if (nestedReplyTo == null || Boolean.TRUE.equals(nestedReplyTo.getIsDeleted())) {
                throw new BusinessException("被嵌套回复的评论不存在");
            }
        }
    }

    /**
     * 根据DTO构建Comment实体
     */
    private Comment buildCommentFromDTO(CommentDTO dto) {
        Comment comment = new Comment();
        CommentSceneEnum scene = CommentSceneEnum.fromCode(dto.getSceneType());

        // 设置场景标记和ID（4选1）
        comment.setInMusic(scene == CommentSceneEnum.MUSIC);
        comment.setInPlaylist(scene == CommentSceneEnum.PLAYLIST);
        comment.setInSpace(scene == CommentSceneEnum.SPACE);

        comment.setMusicId(scene == CommentSceneEnum.MUSIC ? dto.getSceneId() : null);
        comment.setPlaylistId(scene == CommentSceneEnum.PLAYLIST ? dto.getSceneId() : null);
        comment.setSpaceId(scene == CommentSceneEnum.SPACE ? dto.getSceneId() : null);

        // 用户信息
        comment.setUserId(dto.getUserId());
        comment.setUserName(dto.getUserName());

        // 内容
        comment.setContent(dto.getContent().trim());

        // 回复信息
        comment.setIsReply(dto.getIsReply() != null ? dto.getIsReply() : false);
        comment.setReplyUserId(dto.getReplyUserId());
        comment.setReplyCommentId(dto.getReplyCommentId());

        // 嵌套回复信息
        comment.setIsNestedReply(dto.getIsNestedReply() != null ? dto.getIsNestedReply() : false);
        comment.setNestedReplyUserId(dto.getNestedReplyUserId());
        comment.setNestedReplyCommentId(dto.getNestedReplyCommentId());

        return comment;
    }

    /**
     * 更新被回复评论的answer_count
     */
    private void updateAnswerCount(Comment comment) {
        if (Boolean.TRUE.equals(comment.getIsReply()) && comment.getReplyCommentId() != null) {
            Comment replyTo = commentMapper.selectById(comment.getReplyCommentId());
            if (replyTo != null) {
                replyTo.setAnswerCount(replyTo.getAnswerCount() + 1);
                commentMapper.updateById(replyTo);
            }
        }
    }

    /**
     * 标准化DTO参数
     */
    private CommentPageDTO normalizeDto(CommentPageDTO dto) {
        if (dto == null) {
            dto = new CommentPageDTO();
        }
        if (dto.getPageNum() == null || dto.getPageNum() < 1) {
            dto.setPageNum(1L);
        }
        if (dto.getPageSize() == null || dto.getPageSize() < 1) {
            dto.setPageSize(10L);
        }
        if (dto.getPageSize() > 200) {
            dto.setPageSize(200L);
        }
        dto.setSortBy(SortUtils.normalizeSortBy(dto.getSortBy()));
        dto.setSortOrder(SortUtils.normalizeSortOrder(dto.getSortBy(), dto.getSortOrder()));
        return dto;
    }

    /**
     * 转换为VO
     */
    private CommentVO convertToVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());

        vo.setInMusic(comment.getInMusic());
        vo.setInPlaylist(comment.getInPlaylist());
        vo.setInSpace(comment.getInSpace());
        vo.setMusicId(comment.getMusicId());
        vo.setPlaylistId(comment.getPlaylistId());
        vo.setSpaceId(comment.getSpaceId());

        // 场景信息
        if (Boolean.TRUE.equals(comment.getInMusic())) {
            vo.setSceneType("music");
            vo.setSceneId(comment.getMusicId());
        } else if (Boolean.TRUE.equals(comment.getInPlaylist())) {
            vo.setSceneType("playlist");
            vo.setSceneId(comment.getPlaylistId());
        } else if (Boolean.TRUE.equals(comment.getInSpace())) {
            vo.setSceneType("space");
            vo.setSceneId(comment.getSpaceId());
        }

        vo.setUserId(comment.getUserId());
        vo.setUserName(comment.getUserName());
        vo.setContent(comment.getContent());
        vo.setLikeIds(comment.getLikeIds());
        vo.setDislikeIds(comment.getDislikeIds());
        vo.setLikeCount(comment.getLikeCount());
        vo.setDislikeCount(comment.getDislikeCount());
        vo.setAnswerCount(comment.getAnswerCount());
        vo.setIsReply(comment.getIsReply());
        vo.setReplyUserId(comment.getReplyUserId());
        vo.setReplyCommentId(comment.getReplyCommentId());
        vo.setIsNestedReply(comment.getIsNestedReply());
        vo.setNestedReplyUserId(comment.getNestedReplyUserId());
        vo.setNestedReplyCommentId(comment.getNestedReplyCommentId());
        vo.setSafety(comment.getSafety());
        vo.setIsRecommended(comment.getIsRecommended());
        vo.setIsDeleted(comment.getIsDeleted());
        vo.setCreateTime(comment.getCreateTime());
        vo.setUpdateTime(comment.getUpdateTime());

        return vo;
    }

    /**
     * 发送评论相关通知（回复通知 + 作品评论通知 + @mention通知）
     */
    private void sendCommentNotifications(Comment comment, CommentDTO dto) {
        Integer senderId = comment.getUserId();
        String senderName = comment.getUserName();

        // 1. 回复通知
        if (Boolean.TRUE.equals(comment.getIsReply()) && comment.getReplyUserId() != null) {
            notificationService.sendNotification(
                    comment.getReplyUserId(), "reply", senderId,
                    "comment", Long.valueOf(comment.getId()),
                    "评论回复", senderName + " 回复了你的评论");
        }

        // 2. 作品评论通知（非回复时通知作品作者）
        if (!Boolean.TRUE.equals(comment.getIsReply())) {
            Integer ownerId = null;
            String sourceType = null;
            Long sourceId = null;
            String sourceName = null;

            if (Boolean.TRUE.equals(comment.getInMusic()) && comment.getMusicId() != null) {
                Music music = musicMapper.selectById(comment.getMusicId());
                if (music != null && music.getAuthorIds() != null && !music.getAuthorIds().isEmpty()) {
                    ownerId = music.getAuthorIds().get(0);
                }
                sourceType = "music";
                sourceId = Long.valueOf(comment.getMusicId());
                sourceName = music != null ? music.getMusicName() : "";
            } else if (Boolean.TRUE.equals(comment.getInPlaylist()) && comment.getPlaylistId() != null) {
                Playlist playlist = playlistMapper.selectById(comment.getPlaylistId());
                if (playlist != null) {
                    ownerId = playlist.getUserId();
                }
                sourceType = "playlist";
                sourceId = Long.valueOf(comment.getPlaylistId());
                sourceName = playlist != null ? playlist.getPlaylistName() : "";
            }

            if (ownerId != null && !ownerId.equals(senderId)) {
                notificationService.sendNotification(
                        ownerId, "comment", senderId,
                        sourceType, sourceId,
                        "作品评论", senderName + " 评论了你的「" + sourceName + "」");
            }
        }

        // 3. @mention 通知
        if (StringUtils.hasText(comment.getContent())) {
            List<String> mentions = parseMentions(comment.getContent());
            for (String name : mentions) {
                User mentionedUser = findUserByName(name);
                if (mentionedUser != null && !mentionedUser.getId().equals(senderId)) {
                    notificationService.sendNotification(
                            mentionedUser.getId(), "mention", senderId,
                            "comment", Long.valueOf(comment.getId()),
                            "提到你", senderName + " 在评论中提到了你");
                }
            }
        }
    }

    /**
     * 从评论内容中解析 @用户名
     */
    private List<String> parseMentions(String content) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([^\\s@,，.!?;:\\[\\]()]+)");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        java.util.LinkedHashSet<String> mentions = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            mentions.add(matcher.group(1).trim());
        }
        return List.copyOf(mentions);
    }

    /**
     * 根据用户名或昵称查找用户
     */
    private User findUserByName(String name) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getName, name).or().eq(User::getUsername, name);
        wrapper.eq(User::getIsDeleted, false);
        wrapper.last("LIMIT 1");
        return userMapper.selectOne(wrapper);
    }

    /**
     * 更新被评论作品的评论数及热度增量
     */
    private void updateTargetCommentCount(Comment comment) {
        if (Boolean.TRUE.equals(comment.getInMusic()) && comment.getMusicId() != null) {
            Music update = new Music();
            update.setId(comment.getMusicId());
            musicMapper.update(
                    update,
                    new LambdaUpdateWrapper<Music>()
                            .eq(Music::getId, comment.getMusicId())
                            .setSql("comment_count = comment_count + 1")
            );
            dailyStatsService.recordComment("song", Long.valueOf(comment.getMusicId()));
        } else if (Boolean.TRUE.equals(comment.getInPlaylist()) && comment.getPlaylistId() != null) {
            Playlist update = new Playlist();
            update.setId(comment.getPlaylistId());
            playlistMapper.update(
                    update,
                    new LambdaUpdateWrapper<Playlist>()
                            .eq(Playlist::getId, comment.getPlaylistId())
                            .setSql("comment_count = comment_count + 1")
            );
            dailyStatsService.recordComment("playlist", Long.valueOf(comment.getPlaylistId()));
        } else if (Boolean.TRUE.equals(comment.getInSpace()) && comment.getSpaceId() != null) {
            SpacePost update = new SpacePost();
            update.setId(Long.valueOf(comment.getSpaceId()));
            spacePostMapper.update(
                    update,
                    new LambdaUpdateWrapper<SpacePost>()
                            .eq(SpacePost::getId, comment.getSpaceId())
                            .setSql("comment_count = comment_count + 1")
            );
        }
    }

    /**
     * 回退被删除评论对应作品的评论数
     */
    private void rollbackTargetCommentCount(Comment comment) {
        if (Boolean.TRUE.equals(comment.getInMusic()) && comment.getMusicId() != null) {
            Music update = new Music();
            update.setId(comment.getMusicId());
            musicMapper.update(
                    update,
                    new LambdaUpdateWrapper<Music>()
                            .eq(Music::getId, comment.getMusicId())
                            .setSql("comment_count = GREATEST(0, comment_count - 1)")
            );
        } else if (Boolean.TRUE.equals(comment.getInPlaylist()) && comment.getPlaylistId() != null) {
            Playlist update = new Playlist();
            update.setId(comment.getPlaylistId());
            playlistMapper.update(
                    update,
                    new LambdaUpdateWrapper<Playlist>()
                            .eq(Playlist::getId, comment.getPlaylistId())
                            .setSql("comment_count = GREATEST(0, comment_count - 1)")
            );
        } else if (Boolean.TRUE.equals(comment.getInSpace()) && comment.getSpaceId() != null) {
            SpacePost update = new SpacePost();
            update.setId(Long.valueOf(comment.getSpaceId()));
            spacePostMapper.update(
                    update,
                    new LambdaUpdateWrapper<SpacePost>()
                            .eq(SpacePost::getId, comment.getSpaceId())
                            .setSql("comment_count = GREATEST(0, comment_count - 1)")
            );
        }
    }

    /**
     * 回退父评论的回复数
     */
    private void rollbackAnswerCount(Comment comment) {
        if (Boolean.TRUE.equals(comment.getIsReply()) && comment.getReplyCommentId() != null) {
            Comment replyTo = commentMapper.selectById(comment.getReplyCommentId());
            if (replyTo != null && replyTo.getAnswerCount() != null && replyTo.getAnswerCount() > 0) {
                replyTo.setAnswerCount(replyTo.getAnswerCount() - 1);
                commentMapper.updateById(replyTo);
            }
        }
    }
}
