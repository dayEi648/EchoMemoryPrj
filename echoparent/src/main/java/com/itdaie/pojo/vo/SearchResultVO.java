package com.itdaie.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * 综合搜索结果视图对象。
 * 返回给前端的各类型搜索概览数据。
 */
@Data
public class SearchResultVO {

    /**
     * 匹配的单曲列表（少量，默认6条）
     */
    private List<MusicVO> musics;

    /**
     * 匹配的歌单列表（少量，默认4个）
     */
    private List<PlaylistVO> playlists;

    /**
     * 匹配的专辑列表（少量，默认4个）
     */
    private List<AlbumVO> albums;

    /**
     * 匹配的歌手列表（少量，默认4个）
     */
    private List<UserVO> singers;
}
