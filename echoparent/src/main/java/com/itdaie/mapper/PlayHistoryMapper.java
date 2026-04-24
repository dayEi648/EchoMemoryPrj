package com.itdaie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itdaie.pojo.entity.PlayHistory;
import com.itdaie.pojo.vo.PlayHistoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 播放历史数据访问层接口。
 * 继承 MyBatis-Plus 的 BaseMapper，提供基础的 CRUD 操作。
 */
@Mapper
public interface PlayHistoryMapper extends BaseMapper<PlayHistory> {

    /**
     * 根据用户ID查询播放历史，关联 musics 表获取歌曲名。
     *
     * @param userId 用户ID
     * @return 播放历史视图对象列表
     */
    List<PlayHistoryVO> selectByUserId(@Param("userId") Integer userId);

    /**
     * 根据用户ID分页查询播放历史，关联 musics 表获取歌曲信息。
     *
     * @param userId 用户ID
     * @param page 分页对象
     * @return 分页后的播放历史视图对象
     */
    Page<PlayHistoryVO> selectByUserIdPage(@Param("userId") Integer userId, Page<PlayHistoryVO> page);

    /**
     * 根据用户ID查询播放历史中所有歌曲ID。
     *
     * @param userId 用户ID
     * @return 歌曲ID列表
     */
    List<Integer> selectSongIdsByUserId(@Param("userId") Integer userId);
}
