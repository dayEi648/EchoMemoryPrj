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
}
