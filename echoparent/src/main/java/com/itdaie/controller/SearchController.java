package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.SearchResultVO;
import com.itdaie.service.AlbumService;
import com.itdaie.service.MusicService;
import com.itdaie.service.PlaylistService;
import com.itdaie.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@Slf4j
public class SearchController {

    @Autowired
    private MusicService musicService;

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private UserService userService;

    @GetMapping("/all")
    public Result<SearchResultVO> searchAll(@RequestParam String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Result.success(new SearchResultVO());
        }
        String k = keyword.trim();
        SearchResultVO vo = new SearchResultVO();
        vo.setMusics(musicService.searchMusics(k, 1, 6).getRecords());
        vo.setPlaylists(playlistService.searchPlaylists(k, 1, 4).getRecords());
        vo.setAlbums(albumService.searchAlbums(k, 1, 4).getRecords());
        vo.setSingers(userService.searchSingers(k, 1, 4).getRecords());
        return Result.success(vo);
    }

    @GetMapping("/musics")
    public Result<PageDataVo> searchMusics(@RequestParam String keyword,
                                            @RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "20") int pageSize) {
        if (!StringUtils.hasText(keyword)) {
            return Result.success(new PageDataVo(0L, java.util.List.of()));
        }
        return Result.success(musicService.searchMusics(keyword.trim(), pageNum, pageSize));
    }

    @GetMapping("/playlists")
    public Result<PageDataVo> searchPlaylists(@RequestParam String keyword,
                                               @RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "20") int pageSize) {
        if (!StringUtils.hasText(keyword)) {
            return Result.success(new PageDataVo(0L, java.util.List.of()));
        }
        return Result.success(playlistService.searchPlaylists(keyword.trim(), pageNum, pageSize));
    }

    @GetMapping("/albums")
    public Result<PageDataVo> searchAlbums(@RequestParam String keyword,
                                            @RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "20") int pageSize) {
        if (!StringUtils.hasText(keyword)) {
            return Result.success(new PageDataVo(0L, java.util.List.of()));
        }
        return Result.success(albumService.searchAlbums(keyword.trim(), pageNum, pageSize));
    }

    @GetMapping("/singers")
    public Result<PageDataVo> searchSingers(@RequestParam String keyword,
                                             @RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        if (!StringUtils.hasText(keyword)) {
            return Result.success(new PageDataVo(0L, java.util.List.of()));
        }
        return Result.success(userService.searchSingers(keyword.trim(), pageNum, pageSize));
    }

    @GetMapping("/users")
    public Result<PageDataVo> searchUsers(@RequestParam String keyword,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "20") int pageSize) {
        if (!StringUtils.hasText(keyword)) {
            return Result.success(new PageDataVo(0L, java.util.List.of()));
        }
        return Result.success(userService.searchUsers(keyword.trim(), pageNum, pageSize));
    }
}
