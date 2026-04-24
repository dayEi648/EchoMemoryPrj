package com.itdaie.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 空间说说视图对象
 * 返回给前端的说说数据
 *
 * @author itdaie
 */
@Data
public class SpacePostVO {

    private Long id;

    /**
     * 发布用户ID
     */
    private Integer userId;

    /**
     * 发布用户昵称
     */
    private String userName;

    /**
     * 发布用户头像
     */
    private String userAvatar;

    /**
     * 说说内容
     */
    private String content;

    /**
     * 图片URL列表
     */
    private List<String> images;

    /**
     * 帖子类型：original / forward
     */
    private String postType;

    /**
     * 转发来源说说ID
     */
    private Long sourceId;

    /**
     * 转发来源用户昵称
     */
    private String sourceUserName;

    /**
     * 转发来源用户头像
     */
    private String sourceUserAvatar;

    /**
     * 转发来源内容
     */
    private String sourceContent;

    /**
     * 转发来源图片
     */
    private List<String> sourceImages;

    /**
     * 额外数据
     */
    private String extra;

    /**
     * 是否仅自己可见
     */
    private Boolean isPrivate;

    /**
     * 点赞用户ID数组（用于前端判断当前用户是否已点赞）
     */
    private List<Integer> likeIds;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 转发数
     */
    private Integer forwardCount;

    /**
     * 当前登录用户是否已点赞
     */
    private Boolean liked;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
