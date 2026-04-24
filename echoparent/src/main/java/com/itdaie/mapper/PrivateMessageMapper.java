package com.itdaie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itdaie.pojo.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 私信数据访问层
 *
 * @author itdaie
 */
@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    /**
     * 统计用户未读私信总数
     */
    @Select("SELECT COUNT(*) FROM private_message " +
            "WHERE receiver_id = #{userId} AND is_read = FALSE AND deleted = FALSE")
    long countUnreadByUserId(@Param("userId") Integer userId);

    /**
     * 将某会话中接收者的所有消息标记为已读
     */
    @Update("UPDATE private_message SET is_read = TRUE " +
            "WHERE conversation_key = #{conversationKey} AND receiver_id = #{userId} AND is_read = FALSE")
    int markConversationAsRead(@Param("conversationKey") String conversationKey,
                                @Param("userId") Integer userId);
}
