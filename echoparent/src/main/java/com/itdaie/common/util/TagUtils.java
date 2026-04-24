package com.itdaie.common.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 标签计算工具类
 * 提供从多组标签列表中统计频率并取 TopN 的通用方法。
 */
public class TagUtils {

    /**
     * 统计标签频率并取前 N 名，边界并列全部保留。
     *
     * @param tagLists 多组标签列表（如多首歌曲的标签数组）
     * @param topN     期望取前 N 名
     * @return 频率 >= 第 N 名频率的所有标签（若有并列则结果数量可能超过 N）
     */
    public static List<String> computeTopTagsWithTies(List<List<String>> tagLists, int topN) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (List<String> tags : tagLists) {
            if (tags == null) {
                continue;
            }
            for (String tag : tags) {
                if (StringUtils.hasText(tag)) {
                    freqMap.merge(tag.trim(), 1, Integer::sum);
                }
            }
        }

        if (freqMap.isEmpty()) {
            return List.of();
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(freqMap.entrySet());
        entries.sort((a, b) -> {
            int freqCompare = Integer.compare(b.getValue(), a.getValue());
            if (freqCompare != 0) {
                return freqCompare;
            }
            return a.getKey().compareTo(b.getKey());
        });

        int thresholdIndex = Math.min(topN - 1, entries.size() - 1);
        int thresholdFreq = entries.get(thresholdIndex).getValue();

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : entries) {
            if (entry.getValue() >= thresholdFreq) {
                result.add(entry.getKey());
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * 统计标签频率并取前 N 名，严格最多返回 N 个（不保留并列溢出）。
     *
     * @param tagLists 多组标签列表
     * @param topN     最多返回 N 个
     * @return 按频率降序、名称升序排列的前 N 个标签
     */
    public static List<String> computeTopTagsStrict(List<List<String>> tagLists, int topN) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (List<String> tags : tagLists) {
            if (tags == null) {
                continue;
            }
            for (String tag : tags) {
                if (StringUtils.hasText(tag)) {
                    freqMap.merge(tag.trim(), 1, Integer::sum);
                }
            }
        }

        if (freqMap.isEmpty()) {
            return List.of();
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(freqMap.entrySet());
        entries.sort((a, b) -> {
            int freqCompare = Integer.compare(b.getValue(), a.getValue());
            if (freqCompare != 0) {
                return freqCompare;
            }
            return a.getKey().compareTo(b.getKey());
        });

        return entries.stream()
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();
    }
}
