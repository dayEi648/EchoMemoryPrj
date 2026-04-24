package com.itdaie.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * 专辑新增/编辑数据传输对象。
 * 注意：id 新增时无需传入；collectCount、playCount、hot、createTime、updateTime、isDeleted 由数据库自动管理。
 */
@Data
public class AlbumDTO {
    private Integer id;
    private String albumName;
    private List<Integer> authorIds;
    private List<String> authorNames;
    private String albumDescription;
    private String source;
    private List<String> emoTags;
    private List<String> interestTags;
    private String image1Url;
    private String image2Url;
    private List<Integer> songIds;
    private Boolean isRecommended;
}
