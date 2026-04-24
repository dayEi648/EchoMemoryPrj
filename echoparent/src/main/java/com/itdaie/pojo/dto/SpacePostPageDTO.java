package com.itdaie.pojo.dto;

import lombok.Data;

/**
 * 空间说说分页查询数据传输对象
 *
 * @author itdaie
 */
@Data
public class SpacePostPageDTO {

    /**
     * 页码，默认1
     */
    private Long pageNum;

    /**
     * 每页大小，默认10，最大50
     */
    private Long pageSize;

    /**
     * 查询的用户ID（不传则查当前登录用户）
     */
    private Integer userId;
}
