package org.lin1473.shortlink.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.lin1473.shortlink.admin.common.biz.user.UserContext;
import org.lin1473.shortlink.admin.common.convention.exception.ServiceException;
import org.lin1473.shortlink.admin.common.convention.result.Result;
import org.lin1473.shortlink.admin.dao.entity.GroupDO;
import org.lin1473.shortlink.admin.dao.mapper.GroupMapper;
import org.lin1473.shortlink.admin.remote.ShortLinkRemoteService;
import org.lin1473.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import org.lin1473.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import org.lin1473.shortlink.admin.service.RecycleBinService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * URL 回收站接口实现层
 */
@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl implements RecycleBinService {

    private final GroupMapper groupMapper;

    /**
     * 后续重构为 SpringCloud Feign 调用
     */
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };

    @Override
    public Result<IPage<ShortLinkPageRespDTO>> pageRecycleBinShortLinks(ShortLinkRecycleBinPageReqDTO requestParam) {
        // 首先查询当前用户的所有分组标识gid，t_group是用username进行分表的。
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag, 0);
        List<GroupDO> groupDOList = groupMapper.selectList(queryWrapper);
        if(CollUtil.isEmpty(groupDOList)) {
            throw new ServiceException("用户无分组信息");
        }
        // 将查询到的分组记录，转成对应的格式
        requestParam.setGidList(groupDOList.stream().map(GroupDO::getGid).toList());
        // 调用RemoteService，这里不用写分页查询逻辑
        return shortLinkRemoteService.pageRecycleBinShortLinks(requestParam);
    }
}
