package com.itdaie.service;

import com.itdaie.pojo.dto.PlayHistoryDTO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.PlayHistoryVO;

import java.util.List;

public interface PlayHistoryService {

    /**
     * 记录播放历史。
     * 直接 insert，数据库触发器负责处理去重和上限（保留最近50条）。
     *
     * @param dto 播放历史数据传输对象
     */
    void record(PlayHistoryDTO dto);

    /**
     * 根据用户ID查询播放历史列表。
     *
     * @param userId 用户ID
     * @return 播放历史视图对象列表
     */
    List<PlayHistoryVO> listByUser(Integer userId);

    /**
     * 根据用户ID分页查询播放历史。
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageDataVo pageByUser(Integer userId, int pageNum, int pageSize);
}
