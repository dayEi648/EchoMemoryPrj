package com.itdaie.pojo.dto;

import lombok.Data;

/**
 * 播放历史记录数据传输对象。
 */
@Data
public class PlayHistoryDTO {

    private Integer userId;

    private Integer songId;
}
