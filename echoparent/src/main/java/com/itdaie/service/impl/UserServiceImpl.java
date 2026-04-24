package com.itdaie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itdaie.common.config.JwtService;
import com.itdaie.common.constants.OssFolder;
import com.itdaie.common.exception.BusinessException;
import com.itdaie.common.util.SortUtils;
import com.itdaie.common.util.TagUtils;
import com.itdaie.mapper.AlbumMapper;
import com.itdaie.mapper.MusicMapper;
import com.itdaie.mapper.PlayHistoryMapper;
import com.itdaie.mapper.PlaylistMapper;
import com.itdaie.mapper.UserMapper;
import com.itdaie.pojo.dto.response.AuthResult;
import com.itdaie.pojo.dto.UserDTO;
import com.itdaie.pojo.dto.UserPageDTO;
import com.itdaie.pojo.entity.Music;
import com.itdaie.pojo.entity.Playlist;
import com.itdaie.pojo.entity.User;
import com.itdaie.pojo.vo.AlbumVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.PlaylistVO;
import com.itdaie.pojo.vo.UserVO;
import com.itdaie.service.DailyStatsService;
import com.itdaie.service.NotificationService;
import com.itdaie.service.UserService;
import com.itdaie.utils.OssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    // 排序字段：exp 替代 level（level 是生成列，利用 idx_users_exp_desc 索引）
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("safety", "status", "exp", "create_time", "login_time", "like_count");

    private static final Map<String, SFunction<User, ?>> SORT_FIELD_MAP = buildSortFieldMap();

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private PlaylistMapper playlistMapper;

    @Autowired
    private AlbumMapper albumMapper;

    @Autowired
    private PlayHistoryMapper playHistoryMapper;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DailyStatsService dailyStatsService;

    @Override
    public PageDataVo pageQuery(UserPageDTO dto) {
        UserPageDTO safeDto = normalizeDto(dto);
        String sortBy = safeDto.getSortBy();
        String sortOrder = safeDto.getSortOrder();

        SortUtils.validateSort(sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Page<User> page = new Page<>(safeDto.getPageNum(), safeDto.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 默认只查未删除用户；includeDeleted=true 时不加条件以便列出已注销用户
        if (!Boolean.TRUE.equals(safeDto.getIncludeDeleted())) {
            wrapper.eq(User::getIsDeleted, false);
        }

        wrapper.like(StringUtils.hasText(safeDto.getUsername()), User::getUsername, safeDto.getUsername())
                .like(StringUtils.hasText(safeDto.getName()), User::getName, safeDto.getName())
                .eq(safeDto.getRole() != null, User::getRole, safeDto.getRole())
                .eq(safeDto.getStatus() != null, User::getStatus, safeDto.getStatus())
                .eq(safeDto.getProfessional() != null, User::getProfessional, safeDto.getProfessional());

        SortUtils.buildSort(sortBy, sortOrder, wrapper, SORT_FIELD_MAP);

        Page<User> resultPage = userMapper.selectPage(page, wrapper);
        // 转换为VO后再返回
        List<UserVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        return new PageDataVo(resultPage.getTotal(), voList);
    }

    private UserPageDTO normalizeDto(UserPageDTO dto) {
        if (dto == null) {
            throw new BusinessException("request body is required");
        }
        if (dto.getPageNum() == null || dto.getPageNum() < 1) {
            throw new BusinessException("pageNum must be >= 1");
        }
        if (dto.getPageSize() == null || dto.getPageSize() < 1) {
            throw new BusinessException("pageSize must be >= 1");
        }
        if (dto.getPageSize() > 200) {
            throw new BusinessException("pageSize must be <= 200");
        }
        dto.setSortBy(SortUtils.normalizeSortBy(dto.getSortBy()));
        dto.setSortOrder(SortUtils.normalizeSortOrder(dto.getSortBy(), dto.getSortOrder()));
        return dto;
    }

    private static Map<String, SFunction<User, ?>> buildSortFieldMap() {
        Map<String, SFunction<User, ?>> fieldMap = new HashMap<>();
        fieldMap.put("safety", User::getSafety);
        fieldMap.put("status", User::getStatus);
        fieldMap.put("exp", User::getExp);        // 利用 idx_users_exp_desc 索引
        fieldMap.put("create_time", User::getCreateTime);
        fieldMap.put("login_time", User::getLoginTime);
        fieldMap.put("like_count", User::getLikeCount);
        return fieldMap;
    }

    @Override
    public UserVO getById(Integer id) {
        if (id == null) {
            throw new BusinessException("用户ID不能为空");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToVO(user);
    }

    @Override
    @Transactional
    public void add(UserDTO dto) {
        validateAddDTO(dto);
        checkUsernameUniqueForAdd(dto.getUsername());

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setRole(dto.getRole());
        user.setGender(dto.getGender());
        user.setStatus(dto.getStatus());
        user.setSafety(dto.getSafety());
        user.setIsDeleted(dto.getIsDeleted());
        if (dto.getExp() != null) {
            validateExp(dto.getExp());
            user.setExp(dto.getExp());
        }
        user.setCity(dto.getCity());
        user.setBirth(dto.getBirth());
        user.setDescription(dto.getDescription());
        user.setProfessional(dto.getProfessional());
        // emo_tags / interest_tags 由听歌历史自动计算，不接受手动传入
        user.setFanIds(dto.getFanIds());
        user.setFollowIds(dto.getFollowIds());
        user.setSongIds(dto.getSongIds());
        user.setAvatar(dto.getAvatar());

        userMapper.insert(user);
    }

    @Override
    @Transactional
    public void update(UserDTO dto) {
        validateUpdateDTO(dto);

        Integer id = dto.getId();
        User existingUser = userMapper.selectById(id);
        if (existingUser == null) {
            throw new BusinessException("用户不存在");
        }

        checkUsernameUniqueForUpdate(id, existingUser.getUsername(), dto.getUsername());

        User user = new User();
        user.setId(id);
        if (StringUtils.hasText(dto.getUsername())) {
            user.setUsername(dto.getUsername());
        }
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (StringUtils.hasText(dto.getName())) {
            user.setName(dto.getName());
        }
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        if (dto.getSafety() != null) {
            user.setSafety(dto.getSafety());
        }
        if (dto.getIsDeleted() != null) {
            user.setIsDeleted(dto.getIsDeleted());
        }
        if (dto.getExp() != null) {
            validateExp(dto.getExp());
            user.setExp(dto.getExp());
        }
        if (StringUtils.hasText(dto.getCity())) {
            user.setCity(dto.getCity());
        }
        if (dto.getBirth() != null) {
            user.setBirth(dto.getBirth());
        }
        if (StringUtils.hasText(dto.getDescription())) {
            user.setDescription(dto.getDescription());
        }
        if (dto.getProfessional() != null) {
            user.setProfessional(dto.getProfessional());
        }
        // emo_tags / interest_tags 由听歌历史自动计算，不接受手动传入
        if (dto.getFanIds() != null) {
            user.setFanIds(dto.getFanIds());
        }
        if (dto.getFollowIds() != null) {
            user.setFollowIds(dto.getFollowIds());
        }
        if (dto.getSongIds() != null) {
            user.setSongIds(dto.getSongIds());
        }
        if (StringUtils.hasText(dto.getAvatar())) {
            ossUtil.deleteByPublicUrlIfReplaced(existingUser.getAvatar(), dto.getAvatar());
            user.setAvatar(dto.getAvatar());
        }

        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("删除ID列表不能为空");
        }
        List<Integer> validIds = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (validIds.isEmpty()) {
            throw new BusinessException("删除ID列表不能为空");
        }

        // 检查每个用户是否有关联的歌曲（通过 song_ids 数组）；通过后删除 OSS 头像再删库
        for (Integer userId : validIds) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                continue; // 用户不存在，跳过
            }
            if (user.getSongIds() != null && !user.getSongIds().isEmpty()) {
                throw new BusinessException(
                        String.format("用户 %s 有关联的发行歌曲，无法删除", user.getName()));
            }
            ossUtil.deleteByPublicUrl(user.getAvatar());
        }

        userMapper.deleteByIds(validIds);
    }

    private void validateAddDTO(UserDTO dto) {
        if (dto == null) {
            throw new BusinessException("用户信息不能为空");
        }
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
    }

    private void validateUpdateDTO(UserDTO dto) {
        if (dto == null) {
            throw new BusinessException("用户信息不能为空");
        }
        if (dto.getId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
    }

    private void validateExp(Integer exp) {
        if (exp < 0) {
            throw new BusinessException("exp must be >= 0");
        }
    }

    private void checkUsernameUniqueForAdd(String username) {
        if (userMapper.exists(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username))) {
            throw new BusinessException("用户名已存在");
        }
    }

    private void checkUsernameUniqueForUpdate(Integer id, String existingUsername, String newUsername) {
        if (!StringUtils.hasText(newUsername)) {
            return;
        }
        if (Objects.equals(newUsername, existingUsername)) {
            return;
        }
        if (userMapper.exists(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, newUsername)
                .ne(User::getId, id))) {
            throw new BusinessException("用户名已存在");
        }
    }

    @Override
    @Transactional
    public AuthResult login(UserDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException("用户名或密码不能为空");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        LocalDateTime now = LocalDateTime.now();
        updateLoginTime(user.getId(), now);
        user.setLoginTime(now);
        String token = jwtService.generateToken(user);
        return new AuthResult(convertToVO(user), token);
    }

    @Override
    @Transactional
    public AuthResult register(UserDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException("用户名或密码不能为空");
        }
        checkUsernameUniqueForAdd(dto.getUsername());

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setRole(0);
        user.setGender(dto.getGender());
        user.setStatus(dto.getStatus());
        user.setSafety(dto.getSafety());
        user.setIsDeleted(false);
        user.setCity(dto.getCity());
        user.setBirth(dto.getBirth());
        user.setDescription(dto.getDescription());
        user.setProfessional(dto.getProfessional());
        // emo_tags / interest_tags 由听歌历史自动计算，不接受手动传入
        user.setFanIds(dto.getFanIds());
        user.setFollowIds(dto.getFollowIds());
        user.setSongIds(dto.getSongIds());
        user.setAvatar(dto.getAvatar());

        LocalDateTime now = LocalDateTime.now();
        user.setLoginTime(now);
        userMapper.insert(user);

        // 自动创建"我的喜欢"系统歌单
        Playlist likePlaylist = Playlist.builder()
                .playlistName("我的喜欢")
                .userId(user.getId())
                .userName(user.getName() != null ? user.getName() : user.getUsername())
                .isPrivate(true)
                .isLike(true)
                .songIds(List.of())
                .emoTags(List.of())
                .interestTags(List.of())
                .collectCount(0)
                .playCount(0)
                .hot(0)
                .commentCount(0)
                .build();
        playlistMapper.insert(likePlaylist);

        String token = jwtService.generateToken(user);
        return new AuthResult(convertToVO(user), token);
    }

    @Override
    @Transactional
    public void updateLoginTime(Integer userId) {
        updateLoginTime(userId, LocalDateTime.now());
    }

    /**
     * 仅更新 login_time，避免全量 {@link UserMapper#updateById} 受字段策略影响导致漏更。
     */
    private void updateLoginTime(Integer userId, LocalDateTime loginTime) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.update(
                null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getLoginTime, loginTime));
    }

    @Override
    public List<UserVO> getBatchByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Integer> validIds = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (validIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(validIds).stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public List<UserVO> search(String keyword, Integer limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        int safeLimit = limit != null && limit > 0 ? Math.min(limit, 100) : 10;
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getIsDeleted, false)
                .and(w -> w.like(User::getUsername, keyword)
                        .or()
                        .like(User::getName, keyword))
                .last("LIMIT " + safeLimit);
        List<User> users = userMapper.selectList(wrapper);
        return users.stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    @Transactional
    public void followUser(Integer currentUserId, Integer targetUserId) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new BusinessException("当前用户ID不能为空");
        }
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException("目标用户ID不能为空");
        }
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException("不能关注自己");
        }

        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException("当前用户不存在");
        }
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException("目标用户不存在");
        }

        java.util.LinkedHashSet<Integer> currentFollows = new java.util.LinkedHashSet<>(
                currentUser.getFollowIds() != null ? currentUser.getFollowIds() : List.of());
        if (!currentFollows.add(targetUserId)) {
            throw new BusinessException("已关注该用户");
        }

        java.util.LinkedHashSet<Integer> targetFans = new java.util.LinkedHashSet<>(
                targetUser.getFanIds() != null ? targetUser.getFanIds() : List.of());
        targetFans.add(currentUserId);

        User updateCurrent = new User();
        updateCurrent.setId(currentUserId);
        updateCurrent.setFollowIds(List.copyOf(currentFollows));
        userMapper.updateById(updateCurrent);

        User updateTarget = new User();
        updateTarget.setId(targetUserId);
        updateTarget.setFanIds(List.copyOf(targetFans));
        userMapper.updateById(updateTarget);

        // 发送关注通知
        notificationService.sendNotification(targetUserId, "follow", currentUserId,
                "user", Long.valueOf(currentUserId),
                "新粉丝", currentUser.getName() + " 关注了你");
    }

    @Override
    @Transactional
    public void unfollowUser(Integer currentUserId, Integer targetUserId) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new BusinessException("当前用户ID不能为空");
        }
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException("目标用户ID不能为空");
        }

        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException("当前用户不存在");
        }
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException("目标用户不存在");
        }

        java.util.LinkedHashSet<Integer> currentFollows = new java.util.LinkedHashSet<>(
                currentUser.getFollowIds() != null ? currentUser.getFollowIds() : List.of());
        if (!currentFollows.remove(targetUserId)) {
            throw new BusinessException("未关注该用户");
        }

        java.util.LinkedHashSet<Integer> targetFans = new java.util.LinkedHashSet<>(
                targetUser.getFanIds() != null ? targetUser.getFanIds() : List.of());
        targetFans.remove(currentUserId);

        User updateCurrent = new User();
        updateCurrent.setId(currentUserId);
        updateCurrent.setFollowIds(List.copyOf(currentFollows));
        userMapper.updateById(updateCurrent);

        User updateTarget = new User();
        updateTarget.setId(targetUserId);
        updateTarget.setFanIds(List.copyOf(targetFans));
        userMapper.updateById(updateTarget);
    }

    @Override
    public PageDataVo searchSingers(String keyword, int pageNum, int pageSize) {
        return searchUsersByProfessional(keyword, pageNum, pageSize, true);
    }

    @Override
    public PageDataVo searchUsers(String keyword, int pageNum, int pageSize) {
        return searchUsersByProfessional(keyword, pageNum, pageSize, false);
    }

    @Override
    @Transactional
    public void recomputeUserTagsFromPlayHistory(Integer userId) {
        if (userId == null || userId <= 0) {
            return;
        }

        List<Integer> songIds = playHistoryMapper.selectSongIdsByUserId(userId);
        if (songIds == null || songIds.isEmpty()) {
            User patch = new User();
            patch.setId(userId);
            patch.setEmoTags(List.of());
            patch.setInterestTags(List.of());
            userMapper.updateById(patch);
            return;
        }

        List<Music> musics = musicMapper.selectBatchIds(songIds);

        List<String> topEmoTags = TagUtils.computeTopTagsWithTies(
                musics.stream().map(Music::getEmoTags).toList(), 1);
        List<String> topInterestTags = TagUtils.computeTopTagsStrict(
                musics.stream().map(Music::getInterestTags).toList(), 5);

        User patch = new User();
        patch.setId(userId);
        patch.setEmoTags(topEmoTags);
        patch.setInterestTags(topInterestTags);
        userMapper.updateById(patch);
    }

    private PageDataVo searchUsersByProfessional(String keyword, int pageNum, int pageSize, boolean professional) {
        if (!StringUtils.hasText(keyword)) {
            return new PageDataVo(0L, List.of());
        }
        String k = keyword.trim();
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(User::getIsDeleted, false)
                .eq(User::getProfessional, professional)
                .and(w -> w.like(User::getName, k).or().like(User::getUsername, k));

        // 歌手/用户按经验值排序作为"热度"
        wrapper.orderByDesc(User::getExp);

        Page<User> resultPage = userMapper.selectPage(page, wrapper);
        List<UserVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        return new PageDataVo(resultPage.getTotal(), records);
    }

    @Override
    @Transactional
    public void cancel(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        User existingUser = userMapper.selectById(id);
        if (existingUser == null) {
            throw new BusinessException("用户不存在");
        }
        User user = new User();
        user.setId(id);
        user.setIsDeleted(true);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void restore(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        User existingUser = userMapper.selectById(id);
        if (existingUser == null) {
            throw new BusinessException("用户不存在");
        }
        User user = new User();
        user.setId(id);
        user.setIsDeleted(false);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public UserVO updateProfile(Integer userId, UserDTO dto, MultipartFile avatarFile) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            throw new BusinessException("用户不存在");
        }

        User user = new User();
        user.setId(userId);

        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = ossUtil.upload(avatarFile, OssFolder.AVATAR);
            ossUtil.deleteByPublicUrlIfReplaced(existingUser.getAvatar(), avatarUrl);
            user.setAvatar(avatarUrl);
        }

        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (StringUtils.hasText(dto.getCity())) {
            user.setCity(dto.getCity());
        }
        if (StringUtils.hasText(dto.getDescription())) {
            user.setDescription(dto.getDescription());
        }
        if (StringUtils.hasText(dto.getName())) {
            user.setName(dto.getName());
        }
        if (dto.getBirth() != null) {
            user.setBirth(dto.getBirth());
        }

        userMapper.updateById(user);
        return convertToVO(userMapper.selectById(userId));
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setName(user.getName());
        vo.setRole(user.getRole());
        vo.setGender(user.getGender());
        vo.setStatus(user.getStatus());
        vo.setSafety(user.getSafety());
        vo.setIsDeleted(user.getIsDeleted());
        vo.setExp(user.getExp());
        vo.setLevel(user.getLevel());
        vo.setNextLevelExp(user.getNextLevelExp());
        vo.setLevelProgress(user.getLevelProgress());
        vo.setCity(user.getCity());
        vo.setBirth(user.getBirth());
        vo.setDescription(user.getDescription());
        vo.setProfessional(user.getProfessional());
        vo.setEmoTags(user.getEmoTags());
        vo.setInterestTags(user.getInterestTags());
        vo.setFanIds(user.getFanIds());
        vo.setFollowIds(user.getFollowIds());
        vo.setSongIds(user.getSongIds());
        // 从数组长度计算统计数字
        vo.setFanCount(user.getFanIds() != null ? user.getFanIds().size() : 0);
        vo.setFollowCount(user.getFollowIds() != null ? user.getFollowIds().size() : 0);
        vo.setSongCount(user.getSongIds() != null ? user.getSongIds().size() : 0);
        vo.setCollectPlaylistIds(user.getCollectPlaylistIds());
        vo.setCollectAlbumIds(user.getCollectAlbumIds());
        vo.setCollectPlaylistCount(user.getCollectPlaylistIds() != null ? user.getCollectPlaylistIds().size() : 0);
        vo.setCollectAlbumCount(user.getCollectAlbumIds() != null ? user.getCollectAlbumIds().size() : 0);
        vo.setCollectMusicIds(user.getCollectMusicIds());
        vo.setCollectMusicCount(user.getCollectMusicIds() != null ? user.getCollectMusicIds().size() : 0);
        vo.setLikeCount(user.getLikeCount());
        vo.setAvatar(user.getAvatar());
        vo.setLoginTime(user.getLoginTime());
        vo.setUpdateTime(user.getUpdateTime());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    @Override
    public List<PlaylistVO> getCollectedPlaylists(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        List<Integer> ids = user.getCollectPlaylistIds();
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return playlistMapper.selectBatchIds(ids).stream()
                .map(this::convertToPlaylistVO)
                .toList();
    }

    @Override
    public List<AlbumVO> getCollectedAlbums(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        List<Integer> ids = user.getCollectAlbumIds();
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return albumMapper.selectBatchIds(ids).stream()
                .map(this::convertToAlbumVO)
                .toList();
    }

    @Override
    @Transactional
    public void collectPlaylist(Integer userId, Integer playlistId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        if (playlistId == null || playlistId <= 0) {
            throw new BusinessException("歌单ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        com.itdaie.pojo.entity.Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) {
            throw new BusinessException("歌单不存在");
        }
        java.util.LinkedHashSet<Integer> set = new java.util.LinkedHashSet<>(
                user.getCollectPlaylistIds() != null ? user.getCollectPlaylistIds() : List.of());
        if (!set.add(playlistId)) {
            throw new BusinessException("已收藏该歌单");
        }
        User update = new User();
        update.setId(userId);
        update.setCollectPlaylistIds(List.copyOf(set));
        userMapper.updateById(update);

        // 收藏数 +1
        com.itdaie.pojo.entity.Playlist playlistUpdate = new com.itdaie.pojo.entity.Playlist();
        playlistUpdate.setId(playlistId);
        playlistMapper.update(
                playlistUpdate,
                new LambdaUpdateWrapper<com.itdaie.pojo.entity.Playlist>()
                        .eq(com.itdaie.pojo.entity.Playlist::getId, playlistId)
                        .setSql("collect_count = collect_count + 1")
        );

        // 热度增量：记录当日收藏
        dailyStatsService.recordCollect("playlist", Long.valueOf(playlistId));

        // 发送收藏通知
        if (playlist.getUserId() != null && !playlist.getUserId().equals(userId)) {
            notificationService.sendNotification(playlist.getUserId(), "collect", userId,
                    "playlist", Long.valueOf(playlistId),
                    "歌单被收藏", user.getName() + " 收藏了你的歌单「" + playlist.getPlaylistName() + "」");
        }
    }

    @Override
    @Transactional
    public void uncollectPlaylist(Integer userId, Integer playlistId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        if (playlistId == null || playlistId <= 0) {
            throw new BusinessException("歌单ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        java.util.LinkedHashSet<Integer> set = new java.util.LinkedHashSet<>(
                user.getCollectPlaylistIds() != null ? user.getCollectPlaylistIds() : List.of());
        if (!set.remove(playlistId)) {
            throw new BusinessException("未收藏该歌单");
        }
        User update = new User();
        update.setId(userId);
        update.setCollectPlaylistIds(List.copyOf(set));
        userMapper.updateById(update);

        // 收藏数 -1
        com.itdaie.pojo.entity.Playlist playlistUpdate = new com.itdaie.pojo.entity.Playlist();
        playlistUpdate.setId(playlistId);
        playlistMapper.update(
                playlistUpdate,
                new LambdaUpdateWrapper<com.itdaie.pojo.entity.Playlist>()
                        .eq(com.itdaie.pojo.entity.Playlist::getId, playlistId)
                        .setSql("collect_count = GREATEST(0, collect_count - 1)")
        );

        // 热度增量：取消当日收藏
        dailyStatsService.cancelCollect("playlist", Long.valueOf(playlistId));
    }

    @Override
    @Transactional
    public void collectAlbum(Integer userId, Integer albumId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        if (albumId == null || albumId <= 0) {
            throw new BusinessException("专辑ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        com.itdaie.pojo.entity.Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw new BusinessException("专辑不存在");
        }
        java.util.LinkedHashSet<Integer> set = new java.util.LinkedHashSet<>(
                user.getCollectAlbumIds() != null ? user.getCollectAlbumIds() : List.of());
        if (!set.add(albumId)) {
            throw new BusinessException("已收藏该专辑");
        }
        User update = new User();
        update.setId(userId);
        update.setCollectAlbumIds(List.copyOf(set));
        userMapper.updateById(update);

        // 收藏数 +1
        com.itdaie.pojo.entity.Album albumUpdate = new com.itdaie.pojo.entity.Album();
        albumUpdate.setId(albumId);
        albumMapper.update(
                albumUpdate,
                new LambdaUpdateWrapper<com.itdaie.pojo.entity.Album>()
                        .eq(com.itdaie.pojo.entity.Album::getId, albumId)
                        .setSql("collect_count = collect_count + 1")
        );

        // 热度增量：记录当日收藏
        dailyStatsService.recordCollect("album", Long.valueOf(albumId));

        // 发送收藏通知
        List<Integer> authorIds = album.getAuthorIds();
        if (authorIds != null && !authorIds.isEmpty()) {
            Integer ownerId = authorIds.get(0);
            if (!ownerId.equals(userId)) {
                notificationService.sendNotification(ownerId, "collect", userId,
                        "album", Long.valueOf(albumId),
                        "专辑被收藏", user.getName() + " 收藏了你的专辑「" + album.getAlbumName() + "」");
            }
        }
    }

    @Override
    @Transactional
    public void uncollectAlbum(Integer userId, Integer albumId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        if (albumId == null || albumId <= 0) {
            throw new BusinessException("专辑ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        java.util.LinkedHashSet<Integer> set = new java.util.LinkedHashSet<>(
                user.getCollectAlbumIds() != null ? user.getCollectAlbumIds() : List.of());
        if (!set.remove(albumId)) {
            throw new BusinessException("未收藏该专辑");
        }
        User update = new User();
        update.setId(userId);
        update.setCollectAlbumIds(List.copyOf(set));
        userMapper.updateById(update);

        // 收藏数 -1
        com.itdaie.pojo.entity.Album albumUpdate = new com.itdaie.pojo.entity.Album();
        albumUpdate.setId(albumId);
        albumMapper.update(
                albumUpdate,
                new LambdaUpdateWrapper<com.itdaie.pojo.entity.Album>()
                        .eq(com.itdaie.pojo.entity.Album::getId, albumId)
                        .setSql("collect_count = GREATEST(0, collect_count - 1)")
        );

        // 热度增量：取消当日收藏
        dailyStatsService.cancelCollect("album", Long.valueOf(albumId));
    }

    @Override
    public PageDataVo getCollectedPlaylistsPage(Integer userId, int pageNum, int pageSize) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        if (pageNum < 1) {
            throw new BusinessException("页码必须大于等于1");
        }
        if (pageSize < 1 || pageSize > 50) {
            throw new BusinessException("每页条数必须在1~50之间");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        List<Integer> ids = user.getCollectPlaylistIds();
        if (ids == null || ids.isEmpty()) {
            return new PageDataVo(0L, List.of());
        }
        List<com.itdaie.pojo.entity.Playlist> all = playlistMapper.selectBatchIds(ids);
        long total = all.size();
        int fromIndex = (pageNum - 1) * pageSize;
        if (fromIndex >= total) {
            return new PageDataVo(total, List.of());
        }
        int toIndex = Math.min(fromIndex + pageSize, (int) total);
        List<PlaylistVO> records = all.subList(fromIndex, toIndex).stream()
                .map(this::convertToPlaylistVO)
                .toList();
        return new PageDataVo(total, records);
    }

    @Override
    public PageDataVo getCollectedAlbumsPage(Integer userId, int pageNum, int pageSize) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        if (pageNum < 1) {
            throw new BusinessException("页码必须大于等于1");
        }
        if (pageSize < 1 || pageSize > 50) {
            throw new BusinessException("每页条数必须在1~50之间");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        List<Integer> ids = user.getCollectAlbumIds();
        if (ids == null || ids.isEmpty()) {
            return new PageDataVo(0L, List.of());
        }
        List<com.itdaie.pojo.entity.Album> all = albumMapper.selectBatchIds(ids);
        long total = all.size();
        int fromIndex = (pageNum - 1) * pageSize;
        if (fromIndex >= total) {
            return new PageDataVo(total, List.of());
        }
        int toIndex = Math.min(fromIndex + pageSize, (int) total);
        List<AlbumVO> records = all.subList(fromIndex, toIndex).stream()
                .map(this::convertToAlbumVO)
                .toList();
        return new PageDataVo(total, records);
    }

    private PlaylistVO convertToPlaylistVO(com.itdaie.pojo.entity.Playlist playlist) {
        PlaylistVO vo = new PlaylistVO();
        vo.setId(playlist.getId());
        vo.setPlaylistName(playlist.getPlaylistName());
        vo.setUserId(playlist.getUserId());
        vo.setUserName(playlist.getUserName());
        vo.setIsPrivate(playlist.getIsPrivate());
        vo.setListDescription(playlist.getListDescription());
        vo.setSongIds(playlist.getSongIds());
        vo.setEmoTags(playlist.getEmoTags());
        vo.setInterestTags(playlist.getInterestTags());
        vo.setCollectCount(playlist.getCollectCount());
        vo.setPlayCount(playlist.getPlayCount());
        vo.setIsLike(playlist.getIsLike());
        vo.setHot(playlist.getHot());
        vo.setCommentCount(playlist.getCommentCount());
        vo.setIsRecommended(playlist.getIsRecommended());
        vo.setImageUrl(playlist.getImageUrl());
        vo.setCreateTime(playlist.getCreateTime());
        vo.setUpdateTime(playlist.getUpdateTime());
        return vo;
    }

    private AlbumVO convertToAlbumVO(com.itdaie.pojo.entity.Album album) {
        AlbumVO vo = new AlbumVO();
        vo.setId(album.getId());
        vo.setAlbumName(album.getAlbumName());
        vo.setAuthorIds(album.getAuthorIds());
        vo.setAuthorNames(album.getAuthorNames());
        vo.setAlbumDescription(album.getAlbumDescription());
        vo.setSource(album.getSource());
        vo.setEmoTags(album.getEmoTags());
        vo.setInterestTags(album.getInterestTags());
        vo.setCollectCount(album.getCollectCount());
        vo.setPlayCount(album.getPlayCount());
        vo.setHot(album.getHot());
        vo.setImage1Url(album.getImage1Url());
        vo.setImage2Url(album.getImage2Url());
        vo.setSongIds(album.getSongIds());
        vo.setIsRecommended(album.getIsRecommended());
        vo.setIsDeleted(album.getIsDeleted());
        vo.setCreateTime(album.getCreateTime());
        vo.setUpdateTime(album.getUpdateTime());
        return vo;
    }

    @Override
    public PageDataVo getFollowsPage(Integer userId, int pageNum, int pageSize) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        if (pageNum < 1) {
            throw new BusinessException("页码必须大于等于1");
        }
        if (pageSize < 1 || pageSize > 50) {
            throw new BusinessException("每页条数必须在1~50之间");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        List<Integer> followIds = user.getFollowIds();
        if (followIds == null || followIds.isEmpty()) {
            return new PageDataVo(0L, List.of());
        }
        return pageUserIds(followIds, pageNum, pageSize);
    }

    @Override
    public PageDataVo getFansPage(Integer userId, int pageNum, int pageSize) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        if (pageNum < 1) {
            throw new BusinessException("页码必须大于等于1");
        }
        if (pageSize < 1 || pageSize > 50) {
            throw new BusinessException("每页条数必须在1~50之间");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        List<Integer> fanIds = user.getFanIds();
        if (fanIds == null || fanIds.isEmpty()) {
            return new PageDataVo(0L, List.of());
        }
        return pageUserIds(fanIds, pageNum, pageSize);
    }

    private PageDataVo pageUserIds(List<Integer> ids, int pageNum, int pageSize) {
        int total = ids.size();
        int fromIndex = (pageNum - 1) * pageSize;
        if (fromIndex >= total) {
            return new PageDataVo((long) total, List.of());
        }
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Integer> pageIds = ids.subList(fromIndex, toIndex);
        List<UserVO> records = userMapper.selectBatchIds(pageIds).stream()
                .map(this::convertToVO)
                .toList();
        // 按原始ID顺序重排序（selectBatchIds不保证顺序）
        Map<Integer, UserVO> voMap = records.stream()
                .collect(Collectors.toMap(UserVO::getId, v -> v));
        List<UserVO> sorted = pageIds.stream()
                .map(voMap::get)
                .filter(Objects::nonNull)
                .toList();
        return new PageDataVo((long) total, sorted);
    }
}
