package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.pojo.dto.MusicDTO;
import com.itdaie.pojo.dto.MusicPageDTO;
import com.itdaie.pojo.vo.MusicVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.service.MusicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/musics")
@Slf4j
public class MusicController {

    @Autowired
    private MusicService musicService;

    @GetMapping("/page")
    public Result<PageDataVo> page(MusicPageDTO dto) {
        return Result.success(musicService.pageQuery(dto));
    }

    @GetMapping("/{id}")
    public Result<MusicVO> getById(@PathVariable Integer id) {
        MusicVO musicVO = musicService.getById(id);
        return Result.success(musicVO);
    }

    @PostMapping
    public Result<Void> add(@RequestBody MusicDTO dto) {
        musicService.add(dto);
        return Result.success("新增成功", null);
    }

    @PutMapping
    public Result<Void> update(@RequestBody MusicDTO dto) {
        musicService.update(dto);
        return Result.success("修改成功", null);
    }

    @DeleteMapping
    public Result<Void> delete(@RequestParam List<Integer> ids) {
        musicService.deleteByIds(ids);
        return Result.success("删除成功", null);
    }
}
