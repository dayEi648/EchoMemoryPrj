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
    private Integer exp;
    private Integer level;
    private Integer nextLevelExp;
    private Integer levelProgress;
    private Integer safety;
    private Boolean professional;
    private List<String> tags;
    private String avatar;
    private String city;
    private String description;
    private LocalDate birth;
    private LocalDateTime loginTime;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
}
