package com.itdaie.pojo.dto;

import lombok.Data;

/**
 * 首页轮播推图数据传输对象。
 */
@Data
public class BannerDTO {

    private String title;

    private String description;

    private String targetType;

    private Integer targetId;
}
