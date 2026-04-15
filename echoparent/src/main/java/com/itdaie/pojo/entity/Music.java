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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "musics", autoResultMap = true)
public class Music {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("music_name")
    private String musicName;

    /**
     * 作者ID数组（支持多歌手协作）
     */
    @TableField(value = "author_ids", typeHandler = IntegerListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<Integer> authorIds;

    @TableField("album_id")
    private Integer albumId;

    private Boolean vip;

    @TableField("source")
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

    /**
     * 曲风
     */
    private String style;

    /**
     * 语言数组
     */
    @TableField(value = "languages", typeHandler = StringListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<String> languages;

    /**
     * 乐器数组
     */
    @TableField(value = "instruments", typeHandler = StringListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<String> instruments;

    @TableField("collect_count")
    private Integer collectCount;

    /**
     * 热度值，范围0-1000
     */
    private Integer hot;

    @TableField("comment_count")
    private Integer commentCount;

    @TableField("play_count")
    private Integer playCount;

    @TableField("is_recommended")
    private Boolean isRecommended;

    @TableField("is_deleted")
    private Boolean isDeleted;

    @TableField("release_date")
    private LocalDate releaseDate;

    @TableField("file_url")
    private String fileUrl;

    @TableField("lyrics_url")
    private String lyricsUrl;

    @TableField("image1_url")
    private String image1Url;

    @TableField("image2_url")
    private String image2Url;

    @TableField("image3_url")
    private String image3Url;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
