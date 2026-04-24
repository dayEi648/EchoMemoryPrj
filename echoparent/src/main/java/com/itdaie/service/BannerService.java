package com.itdaie.service;

import com.itdaie.pojo.dto.BannerDTO;
import com.itdaie.pojo.vo.BannerVO;

import java.util.List;

/**
 * 首页轮播推图服务接口。
 */
public interface BannerService {

    /**
     * 获取轮播列表（公开，无需权限）。
     */
    List<BannerVO> list();

    /**
     * 新增轮播推图。
     */
    void add(BannerDTO dto);

    /**
     * 修改轮播推图。
     */
    void update(String id, BannerDTO dto);

    /**
     * 删除轮播推图。
     */
    void delete(String id);

    /**
     * 调整轮播推图顺序。
     *
     * @param ids 按新顺序排列的ID列表
     */
    void reorder(List<String> ids);
}
