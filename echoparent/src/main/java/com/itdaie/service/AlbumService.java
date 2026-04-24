package com.itdaie.service;

import com.itdaie.pojo.dto.AlbumDTO;
import com.itdaie.pojo.dto.AlbumPageDTO;
import com.itdaie.pojo.vo.AlbumVO;
import com.itdaie.pojo.vo.PageDataVo;

import java.util.List;

/**
 * 专辑业务层接口。
 * 提供专辑的CRUD和分页查询功能。
 */
public interface AlbumService {

    /**
     * 专辑分页查询。
     * 支持按专辑名称、作者ID、作者名称、歌曲ID、标签等条件筛选。
     * 支持按收藏数、播放数、热度等字段排序。
     *
     * @param dto 分页查询条件
     * @return 分页数据，包含总记录数和当前页数据
     */
    PageDataVo pageQuery(AlbumPageDTO dto);

    /**
     * 根据ID查询专辑详情。
     *
     * @param id 专辑ID
     * @return 专辑视图对象
     * @throws com.itdaie.common.exception.BusinessException 专辑不存在时抛出
     */
    AlbumVO getById(Integer id);

    /**
     * 增加专辑播放量。
     *
     * @param id 专辑ID
     */
    void increasePlayCount(Integer id);

    /**
     * 新增专辑。
     *
     * @param dto 专辑数据传输对象
     * @throws com.itdaie.common.exception.BusinessException 必填字段缺失时抛出
     */
    void add(AlbumDTO dto);

    /**
     * 编辑专辑信息。
     *
     * @param dto 专辑数据传输对象，id字段必填
     * @throws com.itdaie.common.exception.BusinessException 专辑不存在时抛出
     */
    void update(AlbumDTO dto);

    /**
     * 删除专辑（直接从数据库删除）。
     *
     * @param id 专辑ID
     * @throws com.itdaie.common.exception.BusinessException ID为空或专辑不存在时抛出
     */
    void deletePhysical(Integer id);

    /**
     * 批量物理删除专辑。
     *
     * @param ids 专辑ID列表
     */
    void deleteByIds(List<Integer> ids);

    /**
     * 逻辑删除（下架）专辑。
     *
     * @param id 专辑ID
     * @throws com.itdaie.common.exception.BusinessException ID为空或专辑不存在时抛出
     */
    void deleteLogical(Integer id);

    /**
     * 恢复（上架）已逻辑删除的专辑。
     *
     * @param id 专辑ID
     * @throws com.itdaie.common.exception.BusinessException ID为空或专辑不存在时抛出
     */
    void restore(Integer id);

    /**
     * 按专辑名称模糊搜索。
     *
     * @param keyword 关键词
     * @param limit   限制条数
     * @return 专辑列表（仅含 id 和 albumName）
     */
    List<AlbumVO> search(String keyword, Integer limit);

    /**
     * 重新计算专辑的聚合字段（author_ids, author_names, emo_tags, interest_tags）。
     * 标签取歌曲标签频率最高的前 2 名，若第 2 名边界并列则全部保留。
     *
     * @param albumId 专辑ID
     */
    void recomputeAlbumAggregates(Integer albumId);

    /**
     * 重新计算专辑聚合字段，并合并前端传入的额外标签。
     * 标签取歌曲标签频率最高的前 2 名，若第 2 名边界并列则全部保留；
     * extra 标签在去重后追加到结果末尾。
     *
     * @param albumId          专辑ID
     * @param extraEmoTags     额外情绪标签
     * @param extraInterestTags 额外兴趣标签
     */
    void recomputeAlbumAggregates(Integer albumId, List<String> extraEmoTags, List<String> extraInterestTags);

    /**
     * 全局搜索专辑：albumName(模糊) / emo_tags(精确) / interest_tags(精确)
     * 按热度降序分页返回。
     */
    PageDataVo searchAlbums(String keyword, int pageNum, int pageSize);

    /**
     * 首页推荐专辑。
     * 根据当前用户的情绪标签和兴趣标签推荐未删除专辑，最多5个；
     * 不足时按热度补全。用户无标签时直接按热度取前5。
     *
     * @param userId 当前登录用户ID
     * @return 推荐专辑列表（最多5条）
     */
    List<AlbumVO> homeRecommend(Integer userId);
}
