package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.common.exception.BusinessException;
import com.itdaie.pojo.dto.UserDTO;
import com.itdaie.pojo.dto.UserPageDTO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.UserVO;
import com.itdaie.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;
    /**
     * 使用请求参数方式查询用户分页数据。
     */
    @GetMapping("/page")
    public Result<PageDataVo> page(UserPageDTO dto) {
        try {
            return Result.success(userService.pageQuery(dto));
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
    /**
     * 根据ID查询用户详情。
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Integer id) {
        try {
            UserVO userVO = userService.getById(id);
            return Result.success(userVO);
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
    /**
     * 新增用户。
     *
     * @param dto 用户信息
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> add(@RequestBody UserDTO dto) {
        try {
            userService.add(dto);
            return Result.success("新增成功", null);
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
    /**
     * 编辑用户信息。
     *
     * @param dto 用户信息
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> update(@RequestBody UserDTO dto) {
        try {
            userService.update(dto);
            return Result.success("修改成功", null);
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
    /**
     * 批量删除用户。
     *
     * @param ids 用户ID列表
     * @return 操作结果
     */
    @DeleteMapping
    public Result<Void> delete(@RequestParam List<Integer> ids) {
        try {
            userService.deleteByIds(ids);
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
}
