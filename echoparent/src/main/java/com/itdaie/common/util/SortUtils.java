package com.itdaie.common.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.itdaie.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 排序工具类
 * 提供分页查询中的排序字段校验和构建功能
 */
public class SortUtils {

    /**
     * 校验排序字段和排序方向
     *
     * @param sortBy        排序字段
     * @param sortOrder     排序方向 (asc/desc)
     * @param allowedFields 允许的排序字段白名单
     * @throws BusinessException 校验失败时抛出
     */
    public static void validateSort(String sortBy, String sortOrder, Set<String> allowedFields) {
        if (StringUtils.hasText(sortBy) && !allowedFields.contains(sortBy)) {
            throw new BusinessException("sortBy is invalid");
        }
        if (StringUtils.hasText(sortOrder)) {
            String lower = sortOrder.toLowerCase(Locale.ROOT);
            if (!"asc".equals(lower) && !"desc".equals(lower)) {
                throw new BusinessException("sortOrder is invalid");
            }
        }
    }

    /**
     * 构建排序条件
     *
     * @param sortBy    排序字段
     * @param sortOrder 排序方向 (asc/desc)
     * @param wrapper   查询包装器
     * @param fieldMap  字段名到实体字段方法的映射
     * @param <T>       实体类型
     */
    public static <T> void buildSort(String sortBy, String sortOrder,
                                     LambdaQueryWrapper<T> wrapper,
                                     Map<String, SFunction<T, ?>> fieldMap) {
        if (!StringUtils.hasText(sortBy)) {
            return;
        }
        boolean isAsc = "asc".equals(sortOrder);
        SFunction<T, ?> fieldFunc = fieldMap.get(sortBy);
        if (fieldFunc != null) {
            wrapper.orderBy(true, isAsc, fieldFunc);
        }
    }

    /**
     * 标准化排序参数
     *
     * @param sortBy    排序字段
     * @param sortOrder 排序方向
     * @return 标准化后的排序方向
     */
    public static String normalizeSortOrder(String sortBy, String sortOrder) {
        if (!StringUtils.hasText(sortOrder)) {
            return "desc";
        }
        return sortOrder.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 标准化排序字段
     *
     * @param sortBy 排序字段
     * @return 标准化后的排序字段
     */
    public static String normalizeSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return sortBy;
        }
        return sortBy.trim().toLowerCase(Locale.ROOT);
    }
}
