package com.itdaie.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * 空间说说新增/修改数据传输对象
 *
 * @author itdaie
 */
@Data
public class SpacePostDTO {

    /**
     * 说说ID，修改时必填
     */
    private Long id;

    /**
     * 说说内容
     */
    private String content;

    /**
     * 图片URL列表
     */
    private List<String> images;

    /**
     * 是否仅自己可见
     */
    private Boolean isPrivate;

    /**
     * 额外数据（JSON字符串，如@提及用户ID）
     */
    private String extra;
}
