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
 * 专辑实体类，对应albums表。
 * 存储专辑信息，包含专辑名称、描述、作者、包含的歌曲、标签等。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "albums", autoResultMap = true)
public class Album {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("album_name")
    private String albumName;

    /**
     * 作者ID列表
     */
    @TableField(value = "author_ids", typeHandler = IntegerListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<Integer> authorIds;

    /**
     * 作者名称列表
     */
    @TableField(value = "author_names", typeHandler = StringListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<String> authorNames;

    @TableField("album_description")
    private String albumDescription;

    private String source;

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
     * 热度值，范围0-1000
     */
    private Integer hot;

    @TableField("image1_url")
    private String image1Url;

    @TableField("image2_url")
    private String image2Url;

    /**
     * 包含的歌曲ID列表
     */
    @TableField(value = "song_ids", typeHandler = IntegerListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<Integer> songIds;

    @TableField("is_recommended")
    private Boolean isRecommended;

    @TableField("is_deleted")
    private Boolean isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
