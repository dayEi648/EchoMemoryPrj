package com.itdaie.service;

import com.itdaie.pojo.dto.UserPageDTO;
import com.itdaie.pojo.vo.PageData;

public interface UserService {

    /**
     * 用户分页查询。
     * 入参包含查询条件、分页参数和排序参数，返回当前页数据与总记录数。
     */
    PageData pageQuery(UserPageDTO dto);
}
