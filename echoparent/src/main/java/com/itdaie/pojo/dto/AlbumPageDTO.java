package com.itdaie.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * 专辑分页查询数据传输对象。
 * 支持按多种条件筛选和排序。
 */
@Data
public class AlbumPageDTO {
    private Long pageNum;
    private Long pageSize;

    /**
     * 专辑ID精确查询
     */
    private Integer id;

    /**
     * 专辑名称模糊查询
     */
    private String albumName;

    /**
     * 作者ID数组，可多选；包含任一指定作者即匹配。
     * 使用 && 操作符检查数组交集。
     */
    private List<Integer> authorIds;

    /**
     * 作者昵称模糊查询；匹配任一作者昵称即满足。
     */
    private String authorName;

    /**
     * 歌曲ID数组，可多选；
     * 查询包含指定歌曲ID的专辑（命中任一即满足）。
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
     * 是否推荐
     */
    private Boolean isRecommended;

    /**
     * 是否已删除；null表示查询全部，true查询已删除，false查询未删除
     */
    private Boolean isDeleted;

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
