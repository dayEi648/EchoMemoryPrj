package com.itdaie.pojo.dto;

import lombok.Data;

/**
 * 空间说说转发数据传输对象
 *
 * @author itdaie
 */
@Data
public class SpacePostForwardDTO {

    /**
     * 被转发的原说说ID
     */
    private Long sourceId;

    /**
     * 转发附加文字内容
     */
    private String content;
}
