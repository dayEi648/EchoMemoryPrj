package com.itdaie.pojo.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 音乐视图对象。
 * 返回给前端的音乐数据。
 */
@Data
public class MusicVO {

    private Integer id;
    private String musicName;
    /**
     * 作者ID数组（前端可根据ID查询作者信息）
     */
    private List<Integer> authorIds;
    private Integer albumId;
    private Boolean vip;
    private String source;
    /**
     * 情绪标签数组
     */
    private List<String> emoTags;
    /**
     * 兴趣标签数组
     */
    private List<String> interestTags;
    private String style;
    /**
     * 语言数组
     */
    private List<String> languages;
    /**
     * 乐器数组
     */
    private List<String> instruments;
    /**
     * 收藏数（代替原likeCount）
     */
    private Integer collectCount;
    private Integer hot;
    private Integer commentCount;
    private Integer playCount;
    private Boolean isRecommended;
    private LocalDate releaseDate;
    private String fileUrl;
    private String lyricsUrl;
    private String image1Url;
    private String image2Url;
    private String image3Url;
    private Boolean isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
