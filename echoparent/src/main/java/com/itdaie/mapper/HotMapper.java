package com.itdaie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itdaie.pojo.entity.Hot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 热度表 Mapper
 *
 * @author itdaie
 */
@Mapper
public interface HotMapper extends BaseMapper<Hot> {

    /**
     * 批量插入或更新热度记录（PostgreSQL ON CONFLICT）
     *
     * @param list 热度记录列表
     */
    void batchUpsert(@Param("list") List<Hot> list);

    /**
     * 批量查询歌曲的 hot_level 和 trend
     *
     * @param ids 歌曲ID列表
     * @return 包含 targetId / hotLevel / trend 的映射列表
     */
    @Select("<script>SELECT target_id as targetId, hot_level as hotLevel, trend FROM hot WHERE target_type = 'song' AND target_id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<java.util.Map<String, Object>> selectHotExtrasBySongIds(@Param("ids") java.util.List<Integer> ids);
}
