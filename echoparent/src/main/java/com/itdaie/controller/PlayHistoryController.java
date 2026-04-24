package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.pojo.dto.PlayHistoryDTO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.PlayHistoryVO;
import com.itdaie.service.PlayHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/play-history")
@Slf4j
public class PlayHistoryController {

    @Autowired
    private PlayHistoryService playHistoryService;

    @PostMapping
    public Result<Void> record(@RequestBody PlayHistoryDTO dto) {
        playHistoryService.record(dto);
        return Result.success("记录成功", null);
    }

    @GetMapping
    public Result<List<PlayHistoryVO>> listByUser(@RequestParam Integer userId) {
        List<PlayHistoryVO> list = playHistoryService.listByUser(userId);
        return Result.success(list);
    }

    @GetMapping("/page")
    public Result<PageDataVo> pageByUser(@RequestParam Integer userId,
                                         @RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        PageDataVo pageData = playHistoryService.pageByUser(userId, pageNum, pageSize);
        return Result.success(pageData);
    }

}
