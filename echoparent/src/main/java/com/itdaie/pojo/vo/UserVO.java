package com.itdaie.pojo.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象。
 * 返回给前端的用户数据，排除敏感字段如password。
 */
@Data
public class UserVO {

    private Integer id;
    private String username;
    private String name;
    private Integer role;
    private Integer gender;
    private Integer status;
    private Integer safety;
    private Boolean isDeleted;
    private Integer exp;
    private Integer level;
    private Integer nextLevelExp;
    private Integer levelProgress;
    private String city;
    private LocalDate birth;
    private String description;
    private Boolean professional;
    /**
     * 情绪标签数组
     */
    private List<String> emoTags;
    /**
     * 兴趣标签数组
     */
    private List<String> interestTags;
    /**
     * 粉丝ID数组（用于前端展示或进一步操作）
     */
    private List<Integer> fanIds;
    /**
     * 关注ID数组
     */
    private List<Integer> followIds;
    /**
     * 发行歌曲ID数组
     */
    private List<Integer> songIds;
    /**
     * 粉丝数，由fanIds数组长度计算得出
     */
    private Integer fanCount;
    /**
     * 关注数，由followIds数组长度计算得出
     */
    private Integer followCount;
    /**
     * 发行歌曲数，由songIds数组长度计算得出
     */
    private Integer songCount;
    private Integer likeCount;
    private String avatar;
    private LocalDateTime loginTime;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
}
