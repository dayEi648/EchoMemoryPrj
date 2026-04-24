package com.itdaie.common.enums;

import lombok.Getter;

/**
 * 评论场景枚举
 * 定义评论可以关联的4种业务场景
 *
 * @author itdaie
 */
@Getter
public enum CommentSceneEnum {

    /**
     * 音乐评论
     */
    MUSIC("music", "音乐", "music_id", "in_music"),

    /**
     * 歌单评论
     */
    PLAYLIST("playlist", "歌单", "playlist_id", "in_playlist"),

    /**
     * 空间评论
     */
    SPACE("space", "空间", "space_id", "in_space");

    private final String code;
    private final String desc;
    private final String idField;
    private final String flagField;

    CommentSceneEnum(String code, String desc, String idField, String flagField) {
        this.code = code;
        this.desc = desc;
        this.idField = idField;
        this.flagField = flagField;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 场景代码
     * @return 枚举实例，不存在返回null
     */
    public static CommentSceneEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (CommentSceneEnum scene : values()) {
            if (scene.code.equalsIgnoreCase(code)) {
                return scene;
            }
        }
        return null;
    }

    /**
     * 校验code是否有效
     *
     * @param code 场景代码
     * @return true-有效
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}
