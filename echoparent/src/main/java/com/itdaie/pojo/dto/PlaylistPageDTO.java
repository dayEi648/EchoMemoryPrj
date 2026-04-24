package com.itdaie.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * 歌单分页查询数据传输对象。
 * 支持按多种条件筛选和排序。
 */
@Data
public class PlaylistPageDTO {
    private Long pageNum;
    private Long pageSize;

    /**
     * 歌单名称模糊查询
     */
    private String playlistName;

    /**
     * 所属用户ID精确查询
     */
    private Integer userId;

    /**
     * 用户昵称模糊查询
     */
    private String userName;

    /**
     * 歌曲ID数组，可多选。
     * 查询包含指定歌曲ID的歌单。
     */
    private List<Integer> songIds;

    /**
     * 情绪标签数组，可多选；GET 可用重复参数 emoTags=a&emoTags=b 绑定。
     * 多个关键词之间为 OR：命中任一即满足。
     */
    private List<String> emoTags;

    /**
     * 兴趣标签数组，可多选；GET 可用重复参数 interestTags=a&interestTags=b 绑定。
     * 多个关键词之间为 OR：命中任一即满足。
     */
    private List<String> interestTags;

    /**
     * 是否私有（true=私有，false=公开）
     */
    private Boolean isPrivate;

    /**
     * 是否为"我喜欢"歌单
     */
    private Boolean isLike;

    /**
     * 是否推荐
     */
    private Boolean isRecommended;

    /**
     * 可选排序字段：collect_count/play_count/hot/create_time/update_time。
     * 由后端白名单统一校验。
     */
    private String sortBy;

    /**
     * 排序方向（asc/desc），由后端统一校验。
     * 默认为desc。
     */
    private String sortOrder;
}
