package org.lin1473.shortlink.admin.controller;

import lombok.RequiredArgsConstructor;
import org.lin1473.shortlink.admin.common.convention.result.Result;
import org.lin1473.shortlink.admin.common.convention.result.Results;
import org.lin1473.shortlink.admin.dto.resp.UserRespDTO;
import org.lin1473.shortlink.admin.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制层
 */

@RestController
@RequiredArgsConstructor
public class UserController {

    //使用requiredargeconstructor + final代替autowired
    private final UserService userService;

    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/api/shortlink/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable String username) {
        return Results.success(userService.getUserByUsername(username));
//        if (result == null) {
//            return new Result<UserRespDTO>().setCode(UserErrorCodeEnum.USER_NULL.code()).setMessage(UserErrorCodeEnum.USER_NULL.message());
//        } else {
////            return new Result<UserRespDTO>().setCode("0").setData(result);
//            return Results.success(result);
//        }
    }
}
