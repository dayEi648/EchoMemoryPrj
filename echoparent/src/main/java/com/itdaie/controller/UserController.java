package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.pojo.dto.UserPageDTO;
import com.itdaie.pojo.vo.PageData;
import com.itdaie.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;


    @GetMapping("/page")
    /**
     * 使用请求参数方式查询用户分页数据。
     */
    public Result<PageData> page(UserPageDTO dto) {
        try {
            return Result.success(userService.pageQuery(dto));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }
}
