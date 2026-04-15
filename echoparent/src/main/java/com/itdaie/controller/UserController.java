package com.itdaie.controller;

import com.itdaie.common.Result;
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

    @GetMapping("/page")
    public Result<PageDataVo> page(UserPageDTO dto) {
        return Result.success(userService.pageQuery(dto));
    }

    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Integer id) {
        UserVO userVO = userService.getById(id);
        return Result.success(userVO);
    }

    @PostMapping
    public Result<Void> add(@RequestBody UserDTO dto) {
        userService.add(dto);
        return Result.success("新增成功", null);
    }

    @PutMapping
    public Result<Void> update(@RequestBody UserDTO dto) {
        userService.update(dto);
        return Result.success("修改成功", null);
    }

    @DeleteMapping
    public Result<Void> delete(@RequestParam List<Integer> ids) {
        userService.deleteByIds(ids);
        return Result.success("删除成功", null);
    }
}
