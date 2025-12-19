package org.lin1473.shortlink.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.lin1473.shortlink.project.dao.entity.ShortLinkDO;
import org.lin1473.shortlink.project.dto.req.RecycleBinSaveReqDTO;

/**
 * 回收站管理 接口层
 */
public interface RecycleBinService extends IService<ShortLinkDO> {

    /**
     * 保存回收站
     *
     * @param requestParam 请求参数
     */
    void saveRecycleBin(RecycleBinSaveReqDTO requestParam);
}
