package com.itdaie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itdaie.mapper.UserMapper;
import com.itdaie.pojo.entity.User;
import com.itdaie.pojo.dto.UserPageDTO;
import com.itdaie.pojo.vo.PageData;
import com.itdaie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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

    @Override
    /**
     * 执行带条件和排序的分页查询。
     * 流程：参数标准化与校验 -> 构建查询条件 -> 应用排序 -> 执行分页查询。
     */
    public PageData pageQuery(UserPageDTO dto) {
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
        return PageData.from(resultPage);
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
            throw new IllegalArgumentException("sortBy is invalid");
        }
        if (StringUtils.hasText(sortOrder)) {
            String lower = sortOrder.toLowerCase(Locale.ROOT);
            if (!"asc".equals(lower) && !"desc".equals(lower)) {
                throw new IllegalArgumentException("sortOrder is invalid");
            }
        }
    }

    /**
     * 校验分页必填参数，并标准化排序参数格式。
     */
    private UserPageDTO normalizeDto(UserPageDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (dto.getPageNum() == null || dto.getPageNum() < 1) {
            throw new IllegalArgumentException("pageNum must be >= 1");
        }
        if (dto.getPageSize() == null || dto.getPageSize() < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }
        if (dto.getPageSize() > 200) {
            throw new IllegalArgumentException("pageSize must be <= 200");
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
}
