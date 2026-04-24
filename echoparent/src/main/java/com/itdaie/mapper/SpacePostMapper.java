package com.itdaie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itdaie.pojo.entity.SpacePost;
import org.apache.ibatis.annotations.Mapper;

/**
 * 空间说说数据访问层
 *
 * @author itdaie
 */
@Mapper
public interface SpacePostMapper extends BaseMapper<SpacePost> {
}
