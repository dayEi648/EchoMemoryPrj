package com.itdaie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itdaie.common.exception.UserException;
import com.itdaie.mapper.UserMapper;
import com.itdaie.pojo.dto.UserDTO;
import com.itdaie.pojo.entity.User;
import com.itdaie.pojo.dto.UserPageDTO;
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    /**
     * 前端允许使用的排序字段白名单。
     */
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("safety", "status", "level", "create_time", "login_time");

    /**
     * 外部排序字段名与实体字段访问方法的映射关系。
     */
    private static final Map<String, SFunction<User, ?>> SORT_FIELD_MAP = buildSortFieldMap();

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    /**
     * 执行带条件和排序的分页查询。
     * 流程：参数标准化与校验 -> 构建查询条件 -> 应用排序 -> 执行分页查询。
     */
    public PageDataVo pageQuery(UserPageDTO dto) {
        UserPageDTO safeDto = normalizeDto(dto);
        String sortBy = safeDto.getSortBy();
        String sortOrder = safeDto.getSortOrder();
        validateSort(sortBy, sortOrder);

        Page<User> page = new Page<>(safeDto.getPageNum(), safeDto.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasText(safeDto.getUsername()), User::getUsername, safeDto.getUsername())
                .like(StringUtils.hasText(safeDto.getName()), User::getName, safeDto.getName())
                .eq(safeDto.getRole() != null, User::getRole, safeDto.getRole())
                .eq(safeDto.getStatus() != null, User::getStatus, safeDto.getStatus())
                .eq(safeDto.getProfessional() != null, User::getProfessional, safeDto.getProfessional())
                .eq(safeDto.getSafety() != null, User::getSafety, safeDto.getSafety());

        // 数据库会先执行排序，再执行分页截取，保证分页顺序一致。
        buildSort(sortBy, sortOrder, wrapper);
        Page<User> resultPage = userMapper.selectPage(page, wrapper);
        return PageDataVo.from(resultPage);
    }

    /**
     * 当前端传入排序字段时，动态拼接排序规则。
     */
    private void buildSort(String sortBy, String sortOrder, LambdaQueryWrapper<User> wrapper) {
        if (!StringUtils.hasText(sortBy)) {
            return;
        }
        boolean isAsc = "asc".equals(sortOrder);
        wrapper.orderBy(true, isAsc, SORT_FIELD_MAP.get(sortBy));
    }

    /**
     * 校验排序字段和方向，避免非法语句与注入风险。
     */
    private void validateSort(String sortBy, String sortOrder) {
        if (StringUtils.hasText(sortBy) && !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new UserException("sortBy is invalid");
        }
        if (StringUtils.hasText(sortOrder)) {
            String lower = sortOrder.toLowerCase(Locale.ROOT);
            if (!"asc".equals(lower) && !"desc".equals(lower)) {
                throw new UserException("sortOrder is invalid");
            }
        }
    }

    /**
     * 校验分页必填参数，并标准化排序参数格式。
     */
    private UserPageDTO normalizeDto(UserPageDTO dto) {
        if (dto == null) {
            throw new UserException("request body is required");
        }
        if (dto.getPageNum() == null || dto.getPageNum() < 1) {
            throw new UserException("pageNum must be >= 1");
        }
        if (dto.getPageSize() == null || dto.getPageSize() < 1) {
            throw new UserException("pageSize must be >= 1");
        }
        if (dto.getPageSize() > 200) {
            throw new UserException("pageSize must be <= 200");
        }
        if (StringUtils.hasText(dto.getSortBy())) {
            dto.setSortBy(dto.getSortBy().trim().toLowerCase(Locale.ROOT));
        }
        if (!StringUtils.hasText(dto.getSortOrder())) {
            dto.setSortOrder("desc");
        } else {
            dto.setSortOrder(dto.getSortOrder().trim().toLowerCase(Locale.ROOT));
        }
        return dto;
    }

    /**
     * 构建排序字段映射表，用于安全的动态排序。
     */
    private static Map<String, SFunction<User, ?>> buildSortFieldMap() {
        Map<String, SFunction<User, ?>> fieldMap = new HashMap<>();
        fieldMap.put("safety", User::getSafety);
        fieldMap.put("status", User::getStatus);
        fieldMap.put("level", User::getLevel);
        fieldMap.put("create_time", User::getCreateTime);
        fieldMap.put("login_time", User::getLoginTime);
        return fieldMap;
    }

    @Override
    public UserVO getById(Integer id) {
        if (id == null) {
            throw new UserException("用户ID不能为空");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new UserException("用户不存在");
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
        if (dto.getExp() != null) {
            validateExp(dto.getExp());
            user.setExp(dto.getExp());
        }
        user.setAvatar(dto.getAvatar());
        user.setCity(dto.getCity());
        user.setDescription(dto.getDescription());
        user.setBirth(dto.getBirth());
        user.setTags(dto.getTags());
        user.setProfessional(dto.getProfessional());

        // id、level、safety、createTime、updateTime由数据库自动管理

        userMapper.insert(user);
    }

    @Override
    @Transactional
    public void update(UserDTO dto) {
        validateUpdateDTO(dto);

        Integer id = dto.getId();
        User existingUser = userMapper.selectById(id);
        if (existingUser == null) {
            throw new UserException("用户不存在");
        }

        checkUsernameUniqueForUpdate(id, existingUser.getUsername(), dto.getUsername());

        User user = new User();
        user.setId(id);
        if (StringUtils.hasText(dto.getUsername())) {
            user.setUsername(dto.getUsername());
        }
        // 传入密码则更新，不传则保持原密码不变
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
        if (dto.getExp() != null) {
            validateExp(dto.getExp());
            user.setExp(dto.getExp());
        }
        if (StringUtils.hasText(dto.getAvatar())) {
            user.setAvatar(dto.getAvatar());
        }
        if (StringUtils.hasText(dto.getCity())) {
            user.setCity(dto.getCity());
        }
        if (StringUtils.hasText(dto.getDescription())) {
            user.setDescription(dto.getDescription());
        }
        if (dto.getBirth() != null) {
            user.setBirth(dto.getBirth());
        }
        if (dto.getTags() != null) {
            user.setTags(dto.getTags());
        }
        if (dto.getProfessional() != null) {
            user.setProfessional(dto.getProfessional());
        }

        // updateTime由数据库触发器自动更新

        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new UserException("删除ID列表不能为空");
        }
        List<Integer> validIds = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (validIds.isEmpty()) {
            throw new UserException("删除ID列表不能为空");
        }
        userMapper.deleteByIds(validIds);
    }

    /**
     * 校验新增用户DTO。
     */
    private void validateAddDTO(UserDTO dto) {
        if (dto == null) {
            throw new UserException("用户信息不能为空");
        }
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new UserException("用户名不能为空");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new UserException("密码不能为空");
        }
    }

    /**
     * 校验编辑用户DTO。
     */
    private void validateUpdateDTO(UserDTO dto) {
        if (dto == null) {
            throw new UserException("用户信息不能为空");
        }
        if (dto.getId() == null) {
            throw new UserException("用户ID不能为空");
        }
    }

    /**
     * 校验经验值合法性。
     */
    private void validateExp(Integer exp) {
        if (exp < 0) {
            throw new UserException("exp must be >= 0");
        }
    }

    /**
     * 校验新增用户名唯一性。
     */
    private void checkUsernameUniqueForAdd(String username) {
        if (userMapper.exists(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username))) {
            throw new UserException("用户名已存在");
        }
    }

    /**
     * 校验编辑用户名唯一性。
     * 规则：仅当新旧用户名原始字符串不一致时才做占用校验。
     */
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
            throw new UserException("用户名已存在");
        }
    }

    /**
     * 将User实体转换为UserVO。
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setName(user.getName());
        vo.setRole(user.getRole());
        vo.setGender(user.getGender());
        vo.setStatus(user.getStatus());
        vo.setExp(user.getExp());
        vo.setLevel(user.getLevel());
        vo.setNextLevelExp(user.getNextLevelExp());
        vo.setLevelProgress(user.getLevelProgress());
        vo.setSafety(user.getSafety());
        vo.setProfessional(user.getProfessional());
        vo.setTags(user.getTags());
        vo.setAvatar(user.getAvatar());
        vo.setCity(user.getCity());
        vo.setDescription(user.getDescription());
        vo.setBirth(user.getBirth());
        vo.setLoginTime(user.getLoginTime());
        vo.setUpdateTime(user.getUpdateTime());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
