package com.itdaie.pojo.dto.response;

import com.itdaie.pojo.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录认证结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult {

    private UserVO user;
    private String token;
}
