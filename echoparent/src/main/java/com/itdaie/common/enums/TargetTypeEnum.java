package com.itdaie.common.enums;

import lombok.Getter;

/**
 * 热度目标类型枚举
 * 定义参与热度计算的三类作品
 *
 * @author itdaie
 */
@Getter
public enum TargetTypeEnum {

    /**
     * 歌曲
     */
    SONG("song", "歌曲"),

    /**
     * 歌单
     */
    PLAYLIST("playlist", "歌单"),

    /**
     * 专辑
     */
    ALBUM("album", "专辑");

    private final String code;
    private final String desc;

    TargetTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 类型代码
     * @return 枚举实例，不存在返回null
     */
    public static TargetTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TargetTypeEnum type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 校验code是否有效
     *
     * @param code 类型代码
     * @return true-有效
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}
