package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.pojo.dto.BannerDTO;
import com.itdaie.pojo.vo.BannerVO;
import com.itdaie.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 首页轮播推图控制器。
 */
@RestController
@RequestMapping("/api/banners")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping
    public Result<List<BannerVO>> list() {
        return Result.success(bannerService.list());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Void> add(@RequestBody BannerDTO dto) {
        bannerService.add(dto);
        return Result.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Void> update(@PathVariable String id, @RequestBody BannerDTO dto) {
        bannerService.update(id, dto);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Void> delete(@PathVariable String id) {
        bannerService.delete(id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/order")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Void> reorder(@RequestBody List<String> ids) {
        bannerService.reorder(ids);
        return Result.success("排序成功", null);
    }
}
