package com.itdaie.pojo.vo;

import lombok.Data;

/**
 * 首页轮播推图视图对象。
 */
@Data
public class BannerVO {

    private String id;

    private String title;

    private String description;

    private String targetType;

    private Integer targetId;

    private String coverUrl;

    private String targetName;

    private Integer sortOrder;
}
