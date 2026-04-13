package com.itdaie.service;

import com.itdaie.pojo.dto.UserDTO;
import com.itdaie.pojo.dto.UserPageDTO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.UserVO;

import java.util.List;

public interface UserService {

    /**
     * 用户分页查询。
     * 入参包含查询条件、分页参数和排序参数，返回当前页数据与总记录数。
     */
    PageDataVo pageQuery(UserPageDTO dto);

    /**
     * 根据ID查询用户详情。
     *
     * @param id 用户ID
     * @return 用户视图对象
     * @throws com.itdaie.common.exception.UserException 用户不存在时抛出
     */
    UserVO getById(Integer id);

    /**
     * 新增用户。
     *
     * @param dto 用户数据传输对象
     * @throws com.itdaie.common.exception.UserException 用户名已存在时抛出
     */
    void add(UserDTO dto);

    /**
     * 编辑用户信息。
     *
     * @param dto 用户数据传输对象，id字段必填
     * @throws com.itdaie.common.exception.UserException 用户不存在或用户名已存在时抛出
     */
    void update(UserDTO dto);

    /**
     * 批量删除用户。
     *
     * @param ids 用户ID列表
     * @throws com.itdaie.common.exception.UserException ID列表为空时抛出
     */
    void deleteByIds(List<Integer> ids);
}
