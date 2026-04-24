package com.itdaie.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 专辑视图对象。
 * 返回给前端的专辑数据。
 */
@Data
public class AlbumVO {

    private Integer id;

    /**
     * 专辑名称
     */
    private String albumName;

    /**
     * 作者ID列表
     */
    private List<Integer> authorIds;

    /**
     * 作者名称列表
     */
    private List<String> authorNames;

    /**
     * 专辑描述
     */
    private String albumDescription;

    /**
     * 来源
     */
    private String source;

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
     * 热度值
     */
    private Integer hot;

    /**
     * 封面图片URL
     */
    private String image1Url;

    /**
     * 详情页图片URL
     */
    private String image2Url;

    /**
     * 包含的歌曲ID列表
     */
    private List<Integer> songIds;

    /**
     * 是否推荐
     */
    private Boolean isRecommended;

    /**
     * 是否已逻辑删除（下架）；true 下架，false 上架
     */
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
