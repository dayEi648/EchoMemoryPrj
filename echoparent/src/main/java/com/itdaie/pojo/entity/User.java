package com.itdaie.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.itdaie.common.handler.IntegerListTypeHandler;
import com.itdaie.common.handler.StringListTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "users", autoResultMap = true)
public class User {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String username;

    private String password;

    private String name;

    private Integer role;

    private Integer gender;

    private Integer status;

    private Integer safety;

    private Boolean isDeleted;

    /**
     * 经验值，约束范围 0~100000
     */
    private Integer exp;

    /**
     * 由数据库根据经验值自动计算生成。
     */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer level;

    /**
     * 下一级所需经验，由数据库自动生成。
     */
    @TableField(value = "next_level_exp", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer nextLevelExp;

    /**
     * 等级进度百分比，由数据库自动生成。
     */
    @TableField(value = "level_progress", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer levelProgress;

    private String city;

    private LocalDate birth;

    private String description;

    private Boolean professional;

    /**
     * 情绪标签数组
     */
    @TableField(value = "emo_tags", typeHandler = StringListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<String> emoTags;

    /**
     * 兴趣标签数组
     */
    @TableField(value = "interest_tags", typeHandler = StringListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<String> interestTags;

    /**
     * 粉丝ID数组
     */
    @TableField(value = "fan_ids", typeHandler = IntegerListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<Integer> fanIds;

    /**
     * 关注ID数组
     */
    @TableField(value = "follow_ids", typeHandler = IntegerListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<Integer> followIds;

    /**
     * 发行歌曲ID数组
     */
    @TableField(value = "song_ids", typeHandler = IntegerListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<Integer> songIds;

    /**
     * 受点赞量
     */
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 收藏的歌单ID数组
     */
    @TableField(value = "collect_playlist_ids", typeHandler = IntegerListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<Integer> collectPlaylistIds;

    /**
     * 收藏的专辑ID数组
     */
    @TableField(value = "collect_album_ids", typeHandler = IntegerListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<Integer> collectAlbumIds;

    /**
     * 收藏的音乐ID数组
     */
    @TableField(value = "collect_music_ids", typeHandler = IntegerListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<Integer> collectMusicIds;

    private String avatar;

    @TableField("login_time")
    private LocalDateTime loginTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("create_time")
    private LocalDateTime createTime;
}
