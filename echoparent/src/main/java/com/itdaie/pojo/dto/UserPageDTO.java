package com.itdaie.pojo.dto;

import lombok.Data;

@Data
public class UserPageDTO {

    private Long pageNum;
    private Long pageSize;

    private String username;
    private String name;
    private Integer role;
    private Integer status;
    private Boolean professional;
    private Integer safety;

    /**
     * 可选排序字段，由后端白名单统一校验。
     */
    private String sortBy;

    /**
     * 排序方向，由后端统一校验。
     */
    private String sortOrder;
}
