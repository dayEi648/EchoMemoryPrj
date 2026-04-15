package com.itdaie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itdaie.common.exception.BusinessException;
import com.itdaie.common.util.SortUtils;
import com.itdaie.mapper.UserMapper;
import com.itdaie.pojo.dto.UserDTO;
import com.itdaie.pojo.dto.UserPageDTO;
import com.itdaie.pojo.entity.User;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.UserVO;
import com.itdaie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("safety", "status", "level", "create_time", "login_time", "like_count");

    private static final Map<String, SFunction<User, ?>> SORT_FIELD_MAP = buildSortFieldMap();

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public PageDataVo pageQuery(UserPageDTO dto) {
        UserPageDTO safeDto = normalizeDto(dto);
        String sortBy = safeDto.getSortBy();
        String sortOrder = safeDto.getSortOrder();

        SortUtils.validateSort(sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Page<User> page = new Page<>(safeDto.getPageNum(), safeDto.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

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
        fieldMap.put("level", User::getLevel);
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
        user.setEmoTags(dto.getEmoTags());
        user.setInterestTags(dto.getInterestTags());
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
        if (dto.getEmoTags() != null) {
            user.setEmoTags(dto.getEmoTags());
        }
        if (dto.getInterestTags() != null) {
            user.setInterestTags(dto.getInterestTags());
        }
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

        // 检查每个用户是否有关联的歌曲（通过 song_ids 数组）
        for (Integer userId : validIds) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                continue; // 用户不存在，跳过
            }
            if (user.getSongIds() != null && !user.getSongIds().isEmpty()) {
                throw new BusinessException(
                        String.format("用户 %s 有关联的发行歌曲，无法删除", user.getName()));
            }
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
        vo.setLikeCount(user.getLikeCount());
        vo.setAvatar(user.getAvatar());
        vo.setLoginTime(user.getLoginTime());
        vo.setUpdateTime(user.getUpdateTime());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
