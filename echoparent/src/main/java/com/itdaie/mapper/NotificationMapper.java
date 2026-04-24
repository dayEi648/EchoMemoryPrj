package com.itdaie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itdaie.pojo.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 通知数据访问层
 *
 * @author itdaie
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 统计指定用户的各类型未读通知数量
     */
    @Select("SELECT type, COUNT(*) as cnt FROM notifications " +
            "WHERE user_id = #{userId} AND is_read = FALSE AND deleted = FALSE " +
            "GROUP BY type")
    List<TypeCount> selectUnreadCountByType(@Param("userId") Integer userId);

    /**
     * 将指定ID列表的通知标记为已读
     */
    @Update("UPDATE notifications SET is_read = TRUE, read_time = NOW() " +
            "WHERE id IN (${idList}) AND user_id = #{userId} AND is_read = FALSE")
    int markAsRead(@Param("idList") String idList, @Param("userId") Integer userId);

    /**
     * 按类型将用户的所有未读通知标记为已读
     */
    @Update("<script>UPDATE notifications SET is_read = TRUE, read_time = NOW() " +
            "WHERE user_id = #{userId} AND is_read = FALSE AND deleted = FALSE " +
            "<if test='types != null and types.size() > 0'>" +
            "AND type IN " +
            "<foreach collection='types' item='t' open='(' separator=',' close=')'>#{t}</foreach>" +
            "</if>" +
            "</script>")
    int markAllAsRead(@Param("userId") Integer userId, @Param("types") List<String> types);

    /**
     * 内部类：类型统计
     */
    class TypeCount {
        private String type;
        private Long cnt;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Long getCnt() { return cnt; }
        public void setCnt(Long cnt) { this.cnt = cnt; }
    }
}
