package com.itdaie.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 播放历史视图对象。
 * 返回给前端的播放历史数据，包含关联的歌曲名称。
 */
@Data
public class PlayHistoryVO {

    private Long id;

    private Integer userId;

    private Integer songId;

    private LocalDateTime playedAt;

    /**
     * 歌曲名称，关联 musics 表查询填充
     */
    private String musicName;

    /**
     * 歌曲封面图 URL，关联 musics 表 image1_url
     */
    private String coverUrl;

    /**
     * 歌曲音频文件 URL，关联 musics 表 file_url
     */
    private String fileUrl;

    /**
     * 作者ID数组，关联 musics 表 author_ids
     */
    private List<Integer> authorIds;

    /**
     * 作者名称列表，由 Service 层根据 authorIds 二次查询填充
     */
    private List<String> authorNames;

    /**
     * 专辑ID，关联 musics 表 album_id
     */
    private Integer albumId;

    /**
     * 专辑名称，关联 albums 表 album_name
     */
    private String albumName;
}
