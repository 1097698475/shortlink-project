package org.lin1473.shortlink.admin.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import org.lin1473.shortlink.admin.common.convention.result.Result;
import org.lin1473.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import org.lin1473.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

/**
 * URL 回收站接口层
 */
public interface RecycleBinService {

    /**
     * 分页查询回收站短链接
     * 首先查询当前用户的所有分组，赋值给requestParam，之后调用remoteService（根据分组gid查询所有移植回收站的短链接。）
     *
     * @param requestParam 请求参数。不同于短链接分组查询直接传入一个gid，而是需要传入gid集合（用户的所有gid）
     * @return 返回参数包装
     */
    Result<IPage<ShortLinkPageRespDTO>> pageRecycleBinShortLinks(ShortLinkRecycleBinPageReqDTO requestParam);
}
