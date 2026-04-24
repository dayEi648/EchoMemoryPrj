package com.itdaie.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * 歌单新增/编辑数据传输对象。
 * 注意：id 新增时无需传入；collectCount、playCount、hot、commentCount、createTime、updateTime 由数据库自动管理。
 */
@Data
public class PlaylistDTO {

    /**
     * 歌单ID，编辑时必填，新增时无需传入。
     */
    private Integer id;

    /**
     * 歌单名称，必填
     */
    private String playlistName;

    /**
     * 所属用户ID，必填
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
     * 是否为"我喜欢"歌单（每个用户唯一一个）
     */
    private Boolean isLike;

    /**
     * 是否推荐
     */
    private Boolean isRecommended;

    /**
     * 封面图片URL
     */
    private String imageUrl;
}
