package org.lin1473.shortlink.admin.dto.req;


import lombok.Data;

/**
 * 用户登录接口请求参数
 */
@Data
public class UserLoginReqDTO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户密码
     */
    private String password;
}
