package org.lin1473.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.lin1473.shortlink.project.dao.entity.ShortLinkDO;
import org.lin1473.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import org.lin1473.shortlink.project.dto.req.RecycleBinRemoveReqDTO;
import org.lin1473.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkRecycleBinPageReqDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkPageRespDTO;

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

    /**
     * 分页查询回收站的短链接
     *
     * @param requestParam 分页查询短链接请求参数，是当前用户的gidList，而不是单独一个gid
     * @return 短链接分页返回结果
     */
    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam);

    /**
     * 从回收站恢复短链接
     *
     * @param requestParam 恢复短链接请求参数
     */
    void recoverReycleBin(RecycleBinRecoverReqDTO requestParam);

    /**
     * 从回收站移除短链接
     *
     * @param requestParam 移除短链接请求参数
     */
    void removeRecycleBin(RecycleBinRemoveReqDTO requestParam);
}
