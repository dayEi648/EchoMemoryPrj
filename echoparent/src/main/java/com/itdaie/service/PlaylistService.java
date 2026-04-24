package com.itdaie.service;

import com.itdaie.pojo.dto.PlaylistDTO;
import com.itdaie.pojo.dto.PlaylistPageDTO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.PlaylistVO;

import java.util.List;

/**
 * 歌单业务层接口。
 * 提供歌单的CRUD和分页查询功能。
 */
public interface PlaylistService {

    /**
     * 歌单分页查询。
     * 支持按歌单名称、用户ID、用户昵称、歌曲ID、标签等条件筛选。
     * 支持按收藏数、播放数、热度等字段排序。
     *
     * @param dto 分页查询条件
     * @return 分页数据，包含总记录数和当前页数据
     */
    PageDataVo pageQuery(PlaylistPageDTO dto);

    /**
     * 根据ID查询歌单详情。
     *
     * @param id 歌单ID
     * @return 歌单视图对象
     * @throws com.itdaie.common.exception.BusinessException 歌单不存在时抛出
     */
    PlaylistVO getById(Integer id);

    /**
     * 增加歌单播放量。
     *
     * @param id 歌单ID
     */
    void increasePlayCount(Integer id);

    /**
     * 新增歌单。
     *
     * @param dto 歌单数据传输对象
     * @throws com.itdaie.common.exception.BusinessException 必填字段缺失时抛出
     */
    void add(PlaylistDTO dto);

    /**
     * 编辑歌单信息。
     *
     * @param dto 歌单数据传输对象，id字段必填
     * @throws com.itdaie.common.exception.BusinessException 歌单不存在时抛出
     */
    void update(PlaylistDTO dto);

    /**
     * 根据ID删除歌单。
     *
     * @param id 歌单ID
     * @throws com.itdaie.common.exception.BusinessException ID为空或歌单不存在时抛出
     */
    void deleteById(Integer id);

    /**
     * 批量删除歌单。
     *
     * @param ids 歌单ID列表
     * @throws com.itdaie.common.exception.BusinessException ID列表为空时抛出
     */
    void deleteByIds(List<Integer> ids);

    /**
     * 全局搜索歌单：playlistName(模糊) / emo_tags(精确) / interest_tags(精确)
     * 按热度降序分页返回。
     */
    PageDataVo searchPlaylists(String keyword, int pageNum, int pageSize);

    /**
     * 添加歌曲到歌单，自动根据歌单内全部歌曲重新计算并更新歌单标签，
     * 同时同步更新用户的 collect_music_ids。
     *
     * @param playlistId    歌单ID
     * @param musicId       音乐ID
     * @param operatorUserId 操作者用户ID（必须是歌单创建者）
     * @return 更新后的用户 collect_music_ids
     */
    List<Integer> addSongToPlaylist(Integer playlistId, Integer musicId, Integer operatorUserId);

    /**
     * 从歌单移除歌曲，并同步更新用户的 collect_music_ids。
     *
     * @param playlistId    歌单ID
     * @param musicId       音乐ID
     * @param operatorUserId 操作者用户ID（必须是歌单创建者）
     * @return 更新后的用户 collect_music_ids
     */
    List<Integer> removeSongFromPlaylist(Integer playlistId, Integer musicId, Integer operatorUserId);

    /**
     * 首页推荐歌单。
     * 根据用户的情绪标签和兴趣标签，查询包含任一标签的公开歌单，按热度降序取前5；
     * 不足5个时，从所有公开歌单按热度补全。
     *
     * @param userId 当前用户ID
     * @return 推荐歌单列表（最多5条）
     */
    List<PlaylistVO> homeRecommend(Integer userId);
}
