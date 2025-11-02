package org.lin1473.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.lin1473.shortlink.admin.dao.entity.UserDO;
import org.lin1473.shortlink.admin.dto.req.UserLoginReqDTO;
import org.lin1473.shortlink.admin.dto.req.UserRegisterReqDTO;
import org.lin1473.shortlink.admin.dto.req.UserUpdateReqDTO;
import org.lin1473.shortlink.admin.dto.resp.UserLoginRespDTO;
import org.lin1473.shortlink.admin.dto.resp.UserRespDTO;

/**
 * 用户接口层
 */
public interface UserService extends IService<UserDO> {
    /**
     * 根据用户名返回用户
     * @param username
     * @return 用户返回实体
     */
    UserRespDTO getUserByUsername(String username);

    /**
     * 查询用户名是否可用
     * 用户名存在则不可用
     *
     * @param username
     * @return 用户名可用返回True，否则返回False
     */
    Boolean availableUsername(String username);


    /**
     * 注册用户
     * @param requestParam 注册用户请求参数
     */
    void register(UserRegisterReqDTO requestParam);

    /**
     * 根据用户名修改用户信息
     *
     * @param requestParam  更新用户请求参数
     */
    void update(UserUpdateReqDTO requestParam);

    /**
     * 用户登录
     *
     * @param requestParam 用户登录请求参数
     * @return 用户登录返回参数 Token
     */
    UserLoginRespDTO login(UserLoginReqDTO requestParam);

    /**
     * 检查用户是否登录
     *
     * @param username 用户名
     * @param token    用户登录 Token
     * @return 用户是否登录标识
     */
    Boolean checkLogin(String username, String token);

    /**
     * 退出登录
     *
     * @param username 用户名
     * @param token    用户登录 Token
     */
    void logout(String username, String token);
}
