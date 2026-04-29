package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.common.constants.OssFolder;
import com.itdaie.pojo.dto.response.AuthResult;
import com.itdaie.pojo.dto.UserDTO;
import com.itdaie.pojo.dto.UserPageDTO;
import com.itdaie.pojo.vo.AlbumVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.PlaylistVO;
import com.itdaie.pojo.vo.UserVO;
import com.itdaie.service.UserService;
import com.itdaie.utils.ImageProcessUtil;
import com.itdaie.utils.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private OssUtil ossUtil;

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
    public Result<Void> add(@ModelAttribute UserDTO dto,
                            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile) {
        if (avatarFile != null && !avatarFile.isEmpty()) {
            dto.setAvatar(uploadAvatar(avatarFile));
        }
        userService.add(dto);
        return Result.success("新增成功", null);
    }

    @PutMapping
    public Result<Void> update(@ModelAttribute UserDTO dto,
                               @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile) {
        if (avatarFile != null && !avatarFile.isEmpty()) {
            dto.setAvatar(uploadAvatar(avatarFile));
        }
        userService.update(dto);
        return Result.success("修改成功", null);
    }

    @DeleteMapping
    public Result<Void> delete(@RequestParam List<Integer> ids) {
        userService.deleteByIds(ids);
        return Result.success("删除成功", null);
    }

    @PostMapping("/login")
    public Result<AuthResult> login(@RequestBody UserDTO dto) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/register")
    public Result<AuthResult> register(@RequestBody UserDTO dto) {
        return Result.success(userService.register(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout(jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        userService.updateLoginTime(userId);
        return Result.success("已退出登录", null);
    }

    @GetMapping("/search")
    public Result<List<UserVO>> search(@RequestParam String keyword,
                                       @RequestParam(required = false) Integer limit) {
        return Result.success(userService.search(keyword, limit));
    }

    @GetMapping("/batch")
    public Result<List<UserVO>> getBatchByIds(@RequestParam List<Integer> ids) {
        return Result.success(userService.getBatchByIds(ids));
    }

    @PutMapping("/{id}/delete")
    public Result<Void> cancel(@PathVariable Integer id) {
        userService.cancel(id);
        return Result.success("注销成功", null);
    }

    @PutMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Integer id) {
        userService.restore(id);
        return Result.success("恢复成功", null);
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@ModelAttribute UserDTO dto,
                                        @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                        @RequestParam(value = "cropX", required = false) Integer cropX,
                                        @RequestParam(value = "cropY", required = false) Integer cropY,
                                        @RequestParam(value = "cropW", required = false) Integer cropW,
                                        @RequestParam(value = "cropH", required = false) Integer cropH,
                                        jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        String avatarUrl = null;
        if (avatarFile != null && !avatarFile.isEmpty()) {
            avatarUrl = uploadAvatar(avatarFile, cropX, cropY, cropW, cropH);
        }
        UserVO vo = userService.updateProfile(userId, dto, avatarUrl);
        return Result.success("资料更新成功", vo);
    }

    @GetMapping("/collections/playlists")
    public Result<List<PlaylistVO>> getCollectedPlaylists(jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(userService.getCollectedPlaylists(userId));
    }

    @GetMapping("/collections/albums")
    public Result<List<AlbumVO>> getCollectedAlbums(jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(userService.getCollectedAlbums(userId));
    }

    @PostMapping("/collections/playlists/{id}")
    public Result<Void> collectPlaylist(@PathVariable Integer id,
                                        jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        userService.collectPlaylist(userId, id);
        return Result.success("收藏成功", null);
    }

    @DeleteMapping("/collections/playlists/{id}")
    public Result<Void> uncollectPlaylist(@PathVariable Integer id,
                                          jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        userService.uncollectPlaylist(userId, id);
        return Result.success("取消收藏成功", null);
    }

    @PostMapping("/collections/albums/{id}")
    public Result<Void> collectAlbum(@PathVariable Integer id,
                                     jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        userService.collectAlbum(userId, id);
        return Result.success("收藏成功", null);
    }

    @DeleteMapping("/collections/albums/{id}")
    public Result<Void> uncollectAlbum(@PathVariable Integer id,
                                       jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        userService.uncollectAlbum(userId, id);
        return Result.success("取消收藏成功", null);
    }

    @GetMapping("/collections/playlists/page")
    public Result<PageDataVo> getCollectedPlaylistsPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize,
            jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(userService.getCollectedPlaylistsPage(userId, pageNum, pageSize));
    }

    @GetMapping("/collections/albums/page")
    public Result<PageDataVo> getCollectedAlbumsPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize,
            jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(userService.getCollectedAlbumsPage(userId, pageNum, pageSize));
    }

    @PostMapping("/{id}/follow")
    public Result<Void> followUser(@PathVariable Integer id,
                                   jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        userService.followUser(userId, id);
        return Result.success("关注成功", null);
    }

    @DeleteMapping("/{id}/follow")
    public Result<Void> unfollowUser(@PathVariable Integer id,
                                     jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        userService.unfollowUser(userId, id);
        return Result.success("取消关注成功", null);
    }

    @GetMapping("/follows")
    public Result<PageDataVo> getFollows(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(userService.getFollowsPage(userId, pageNum, pageSize));
    }

    @GetMapping("/fans")
    public Result<PageDataVo> getFans(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(userService.getFansPage(userId, pageNum, pageSize));
    }

    private String uploadAvatar(MultipartFile file) {
        return uploadAvatar(file, null, null, null, null);
    }

    private String uploadAvatar(MultipartFile file, Integer cropX, Integer cropY, Integer cropW, Integer cropH) {
        if (!ImageProcessUtil.isImage(file)) {
            throw new com.itdaie.common.exception.FileUploadException("请上传图片文件");
        }
        try {
            InputStream is;
            if (cropX != null && cropY != null && cropW != null && cropH != null && cropW > 0 && cropH > 0) {
                is = ImageProcessUtil.cropAndCompress(file, cropX, cropY, cropW, cropH, 400, 400, 0.90f);
            } else {
                is = ImageProcessUtil.cropToSquareAndCompress(file, 400, 0.90f);
            }
            return ossUtil.upload(is, OssFolder.AVATAR, ".jpg");
        } catch (Exception e) {
            throw new com.itdaie.common.exception.FileUploadException("头像处理失败: " + e.getMessage());
        }
    }
}
