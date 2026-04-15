package com.itdaie.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 歌单视图对象。
 * 返回给前端的歌单数据。
 */
@Data
public class PlaylistVO {

    private Integer id;

    /**
     * 歌单名称
     */
    private String playlistName;

    /**
     * 所属用户ID
     */
    private Integer userId;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 是否私有（true=私有，false=公开）
     */
    private Boolean isPrivate;

    /**
     * 歌单简介
     */
    private String listDescription;

    /**
     * 包含的歌曲ID数组
     */
    private List<Integer> songIds;

    /**
     * 情绪标签数组
     */
    private List<String> emoTags;

    /**
     * 兴趣标签数组
     */
    private List<String> interestTags;

    /**
     * 收藏数
     */
    private Integer collectCount;

    /**
     * 播放次数
     */
    private Integer playCount;

    /**
     * 是否为"我喜欢"歌单
     */
    private Boolean isLike;

    /**
     * 热度值
     */
    private Integer hot;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 是否推荐
     */
    private Boolean isRecommended;

    /**
     * 封面图片URL
     */
    private String imageUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
