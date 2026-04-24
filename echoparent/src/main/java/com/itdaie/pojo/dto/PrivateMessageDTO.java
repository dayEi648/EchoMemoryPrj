package com.itdaie.pojo.dto;

import lombok.Data;

/**
 * 发送私信数据传输对象
 *
 * @author itdaie
 */
@Data
public class PrivateMessageDTO {

    /**
     * 接收者用户ID
     */
    private Integer receiverId;

    /**
     * 消息内容
     */
    private String content;
}
