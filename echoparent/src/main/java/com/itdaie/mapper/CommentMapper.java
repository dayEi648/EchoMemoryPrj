package com.itdaie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itdaie.pojo.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论数据访问层
 *
 * @author itdaie
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
