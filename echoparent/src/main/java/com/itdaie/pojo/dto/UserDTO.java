package com.itdaie.pojo.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户新增/编辑数据传输对象。
 * 注意：id 新增时无需传入；level、nextLevelExp、levelProgress 由数据库自动管理。
 */
@Data
public class UserDTO {
    /**
     * 用户ID，编辑时必填，新增时无需传入。
     */
    private Integer id;

    /**
     * 用户名，必填，需唯一。
     */
    private String username;

    /**
     * 密码，新增时必填，编辑时传入则更新密码，不传则保持原密码不变。
     */
    private String password;

    private String name;

    private Integer role;

    private Integer gender;

    private Integer status;

    private Integer safety;

    private Boolean isDeleted;

    /**
     * 经验值，可选；传入后由数据库规则联动等级相关字段。
     */
    private Integer exp;

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
     * 粉丝ID数组
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

    private String avatar;
}
