package com.itdaie.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class MusicPageDTO {
    private Long pageNum;
    private Long pageSize;
    private String musicName;

    /**
     * 作者ID数组，可多选
     */
    private List<Integer> authorIds;

    private Integer albumId;
    private Boolean vip;
    private String style;

    /**
     * 可多选；GET 可用重复参数 emoTags=a&emoTags=b 绑定。
     * 多个关键词之间为 OR：命中任一即满足。
     */
    private List<String> emoTags;

    /**
     * 可多选；GET 可用重复参数 interestTags=a&interestTags=b 绑定。
     * 多个关键词之间为 OR：命中任一即满足。
     */
    private List<String> interestTags;

    /**
     * 可多选；GET 可用重复参数 language=a&language=b 绑定。
     * 多个关键词之间为 OR（列 languages）。
     */
    private List<String> language;

    /**
     * 可多选；GET 可用重复参数 instruments=a&instruments=b 绑定。
     * 多个关键词之间为 OR（列 instruments）。
     */
    private List<String> instruments;

    private Boolean isRecommended;
    private Boolean isDeleted;

    /**
     * 可选排序字段，由后端白名单统一校验。
     */
    private String sortBy;

    /**
     * 排序方向，由后端统一校验。
     */
    private String sortOrder;
}
