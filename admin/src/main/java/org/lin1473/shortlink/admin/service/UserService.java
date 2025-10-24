package org.lin1473.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.lin1473.shortlink.admin.dao.entity.UserDO;
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
}
