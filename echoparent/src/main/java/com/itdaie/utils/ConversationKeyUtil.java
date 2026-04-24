package com.itdaie.utils;

/**
 * 私信会话键工具类
 * 确保双方对话使用相同的 conversation_key
 *
 * @author itdaie
 */
public class ConversationKeyUtil {

    /**
     * 根据两个用户ID生成会话键
     * 规则：小ID + ":" + 大ID
     */
    public static String build(Integer userId1, Integer userId2) {
        if (userId1 == null || userId2 == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (userId1 < userId2) {
            return userId1 + ":" + userId2;
        } else {
            return userId2 + ":" + userId1;
        }
    }

    /**
     * 从会话键中解析出对方用户ID
     */
    public static Integer getOtherUserId(String conversationKey, Integer currentUserId) {
        if (conversationKey == null || currentUserId == null) {
            return null;
        }
        String[] parts = conversationKey.split(":");
        if (parts.length != 2) {
            return null;
        }
        int id1 = Integer.parseInt(parts[0]);
        int id2 = Integer.parseInt(parts[1]);
        return currentUserId == id1 ? id2 : id1;
    }
}
