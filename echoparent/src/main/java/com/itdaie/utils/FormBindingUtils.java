package com.itdaie.utils;

import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Multipart 重复字段在「空数组」时前端会传单个空串，绑定后需过滤再写入实体。
 */
public final class FormBindingUtils {

    private FormBindingUtils() {
    }

    public static List<String> normalizeStringList(List<String> raw) {
        if (raw == null) {
            return null;
        }
        return raw.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    public static List<Integer> normalizeIntegerList(List<String> raw) {
        if (raw == null) {
            return null;
        }
        return raw.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();
    }
}
