package com.itdaie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itdaie.pojo.entity.Album;
import org.apache.ibatis.annotations.Mapper;

/**
 * 专辑数据访问层接口。
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作。
 */
@Mapper
public interface AlbumMapper extends BaseMapper<Album> {
}
