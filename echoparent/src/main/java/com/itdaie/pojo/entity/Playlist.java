package com.itdaie.pojo.entity;

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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 歌单实体类，对应playlists表。
 * 存储用户创建的歌单信息，包含歌单名称、描述、包含的歌曲、标签等。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "playlists", autoResultMap = true)
public class Playlist {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("playlist_name")
    private String playlistName;

    @TableField("user_id")
    private Integer userId;

    @TableField("user_name")
    private String userName;

    /**
     * 是否私有（true=私有，false=公开）
     */
    @TableField("private")
    private Boolean isPrivate;

    @TableField("list_description")
    private String listDescription;

    /**
     * 包含的歌曲ID数组
     */
    @TableField(value = "song_ids", typeHandler = IntegerListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<Integer> songIds;

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

    @TableField("collect_count")
    private Integer collectCount;

    @TableField("play_count")
    private Integer playCount;

    /**
     * 是否为"我喜欢"歌单（每个用户唯一一个）
     */
    @TableField("is_like")
    private Boolean isLike;

    /**
     * 热度值，范围0-1000
     */
    private Integer hot;

    @TableField("comment_count")
    private Integer commentCount;

    @TableField("is_recommended")
    private Boolean isRecommended;

    @TableField("image_url")
    private String imageUrl;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
