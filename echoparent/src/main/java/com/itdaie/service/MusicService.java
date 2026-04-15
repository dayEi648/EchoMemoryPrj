package com.itdaie.service;

import com.itdaie.pojo.dto.MusicDTO;
import com.itdaie.pojo.dto.MusicPageDTO;
import com.itdaie.pojo.vo.MusicVO;
import com.itdaie.pojo.vo.PageDataVo;

import java.util.List;

public interface MusicService {

    /**
     * 音乐分页查询。
     * 入参包含查询条件、分页参数和排序参数，返回当前页数据与总记录数。
     */
    PageDataVo pageQuery(MusicPageDTO dto);

    /**
     * 根据ID查询音乐详情。
     *
     * @param id 音乐ID
     * @return 音乐视图对象
     * @throws com.itdaie.common.exception.MusicException 音乐不存在时抛出
     */
    MusicVO getById(Integer id);

    /**
     * 新增音乐。
     *
     * @param dto 音乐数据传输对象
     * @throws com.itdaie.common.exception.MusicException 必填字段缺失时抛出
     */
    void add(MusicDTO dto);

    /**
     * 编辑音乐信息。
     *
     * @param dto 音乐数据传输对象，id字段必填
     * @throws com.itdaie.common.exception.MusicException 音乐不存在时抛出
     */
    void update(MusicDTO dto);

    /**
     * 批量删除音乐。
     *
     * @param ids 音乐ID列表
     * @throws com.itdaie.common.exception.MusicException ID列表为空时抛出
     */
    void deleteByIds(List<Integer> ids);
}
