package com.itdaie.service;

import com.itdaie.pojo.dto.response.AuthResult;
import com.itdaie.pojo.dto.UserDTO;
import com.itdaie.pojo.dto.UserPageDTO;
import com.itdaie.pojo.vo.AlbumVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.PlaylistVO;
import com.itdaie.pojo.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 用户登录。
     *
     * @param dto 包含用户名和密码
     * @return 用户资料与JWT令牌
     * @throws com.itdaie.common.exception.BusinessException 用户不存在或密码错误时抛出
     */
    AuthResult login(UserDTO dto);

    /**
     * 用户注册。
     *
     * @param dto 包含用户名、密码等注册信息
     * @return 用户资料与JWT令牌
     * @throws com.itdaie.common.exception.BusinessException 用户名已存在时抛出
     */
    AuthResult register(UserDTO dto);

    /**
     * 将用户 {@code login_time} 更新为当前时间（登录成功、注册后首次会话、退出登录时调用）。
     *
     * @param userId 用户 ID
     * @throws com.itdaie.common.exception.BusinessException 用户不存在时抛出
     */
    void updateLoginTime(Integer userId);

    /**
     * 按用户名/昵称模糊搜索用户。
     *
     * @param keyword 关键词
     * @param limit   返回条数上限
     * @return 用户列表
     */
    List<UserVO> search(String keyword, Integer limit);

    /**
     * 根据ID批量查询用户。
     *
     * @param ids 用户ID列表
     * @return 用户视图对象列表
     */
    List<UserVO> getBatchByIds(List<Integer> ids);

    /**
     * 逻辑删除（注销）用户。
     *
     * @param id 用户ID
     * @throws com.itdaie.common.exception.BusinessException 用户不存在时抛出
     */
    void cancel(Integer id);

    /**
     * 恢复已注销用户。
     *
     * @param id 用户ID
     * @throws com.itdaie.common.exception.BusinessException 用户不存在时抛出
     */
    void restore(Integer id);

    /**
     * 当前登录用户更新个人资料（头像、性别、城市、简介等）。
     *
     * @param userId    当前用户ID
     * @param dto       资料数据
     * @param avatarFile 头像文件（可选）
     * @return 更新后的用户视图对象
     * @throws com.itdaie.common.exception.BusinessException 用户不存在时抛出
     */
    UserVO updateProfile(Integer userId, UserDTO dto, MultipartFile avatarFile);

    /**
     * 查询当前用户收藏的歌单列表。
     */
    List<PlaylistVO> getCollectedPlaylists(Integer userId);

    /**
     * 查询当前用户收藏的专辑列表。
     */
    List<AlbumVO> getCollectedAlbums(Integer userId);

    /**
     * 收藏歌单。
     */
    void collectPlaylist(Integer userId, Integer playlistId);

    /**
     * 取消收藏歌单。
     */
    void uncollectPlaylist(Integer userId, Integer playlistId);

    /**
     * 收藏专辑。
     */
    void collectAlbum(Integer userId, Integer albumId);

    /**
     * 取消收藏专辑。
     */
    void uncollectAlbum(Integer userId, Integer albumId);

    /**
     * 分页查询当前用户收藏的歌单。
     */
    PageDataVo getCollectedPlaylistsPage(Integer userId, int pageNum, int pageSize);

    /**
     * 分页查询当前用户收藏的专辑。
     */
    PageDataVo getCollectedAlbumsPage(Integer userId, int pageNum, int pageSize);

    /**
     * 搜索歌手：name/username(模糊) 且 professional=true
     * 按热度(exp)降序分页返回。
     */
    PageDataVo searchSingers(String keyword, int pageNum, int pageSize);

    /**
     * 搜索普通用户：name/username(模糊) 且 professional=false
     * 按热度(exp)降序分页返回。
     */
    PageDataVo searchUsers(String keyword, int pageNum, int pageSize);

    /**
     * 根据用户听歌历史重新计算并更新其 emo_tags 与 interest_tags。
     * 情绪标签取频率最高的 1 个，若有并列则全部保留；
     * 兴趣标签取频率最高的前 5 个，最多 5 个。
     *
     * @param userId 用户ID
     */
    void recomputeUserTagsFromPlayHistory(Integer userId);

    /**
     * 关注用户。
     * 将当前用户的ID加入目标用户的fan_ids，将目标用户ID加入当前用户的follow_ids。
     *
     * @param currentUserId 当前登录用户ID
     * @param targetUserId  目标用户ID
     * @throws com.itdaie.common.exception.BusinessException 用户不存在、关注自己或已关注时抛出
     */
    void followUser(Integer currentUserId, Integer targetUserId);

    /**
     * 取消关注用户。
     * 从双方数组中移除对应ID。
     *
     * @param currentUserId 当前登录用户ID
     * @param targetUserId  目标用户ID
     * @throws com.itdaie.common.exception.BusinessException 用户不存在或未关注时抛出
     */
    void unfollowUser(Integer currentUserId, Integer targetUserId);

    /**
     * 分页查询当前用户的关注列表。
     *
     * @param userId   当前用户ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页用户列表
     */
    PageDataVo getFollowsPage(Integer userId, int pageNum, int pageSize);

    /**
     * 分页查询当前用户的粉丝列表。
     *
     * @param userId   当前用户ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页用户列表
     */
    PageDataVo getFansPage(Integer userId, int pageNum, int pageSize);
}
