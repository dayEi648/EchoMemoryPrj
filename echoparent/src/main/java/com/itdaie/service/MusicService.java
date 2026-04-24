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

    /**
     * 按音乐名模糊搜索。
     *
     * @param keyword 搜索关键词
     * @param limit   限制条数
     * @return 音乐视图对象列表
     */
    List<MusicVO> search(String keyword, Integer limit);

    /**
     * 全局搜索音乐：musicName(模糊) / 作者名→user.song_ids / 专辑名→album.song_ids /
     * style(精确) / emo_tags(精确) / interest_tags(精确) / languages(精确) / instruments(精确)
     * 8字段取并集，按热度降序分页返回。
     */
    PageDataVo searchMusics(String keyword, int pageNum, int pageSize);

    /**
     * 根据ID批量查询音乐。
     *
     * @param ids 音乐ID列表
     * @return 音乐视图对象列表
     */
    List<MusicVO> getBatchByIds(List<Integer> ids);

    /**
     * 首页热门音乐。
     * 取热度最高的6首音乐，结果缓存到 Redis（当天有效）。
     *
     * @return 热门音乐列表（最多6条）
     */
    List<MusicVO> homeHotMusics();

    /**
     * 首页新歌速递。
     * 取近7天内发布的音乐，按热度降序取前6首；不足6首不补全。
     *
     * @return 新歌列表（最多6条）
     */
    List<MusicVO> homeNewMusics();

    /**
     * 获取高频标签列表。
     * 从所有未删除音乐中统计指定类型标签的出现频率，返回频率最高的前 limit 个。
     * 结果缓存到 Redis（TTL=1小时）。
     *
     * @param type  标签类型：emotion / interest / style / instrument / language
     * @param limit 返回数量上限
     * @return 标签列表，按频率降序排列
     */
    List<String> getTopTags(String type, Integer limit);
}
