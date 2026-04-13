package com.itdaie.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("\"user\"")
public class User {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String password;
    private String name;
    private Integer role;
    private Integer gender;
    private Integer status;

    private Integer exp;
    /**
     * 由数据库根据经验值自动计算生成。
     */
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
