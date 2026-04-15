package com.itdaie.pojo.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 音乐新增/编辑数据传输对象。
 * 注意：id 新增时无需传入；collectCount、commentCount、playCount、hot、createTime、updateTime 由数据库自动管理。
 */
@Data
public class MusicDTO {

    /**
     * 音乐ID，编辑时必填，新增时无需传入。
     */
    private Integer id;

    private String musicName;

    /**
     * 作者ID数组（支持多歌手协作）
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

    private LocalDate releaseDate;

    private String fileUrl;

    private String lyricsUrl;

    private String image1Url;

    private String image2Url;

    private String image3Url;

    private Boolean isRecommended;

    private Boolean isDeleted;
}
