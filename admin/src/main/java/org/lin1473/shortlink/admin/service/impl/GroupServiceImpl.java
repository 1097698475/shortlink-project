package org.lin1473.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.lin1473.shortlink.admin.common.biz.user.UserContext;
import org.lin1473.shortlink.admin.common.convention.result.Result;
import org.lin1473.shortlink.admin.dao.entity.GroupDO;
import org.lin1473.shortlink.admin.dao.mapper.GroupMapper;
import org.lin1473.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import org.lin1473.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import org.lin1473.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import org.lin1473.shortlink.admin.remote.ShortLinkRemoteService;
import org.lin1473.shortlink.admin.remote.dto.resp.ShortLinkGroupCountQueryRespDTO;
import org.lin1473.shortlink.admin.service.GroupService;
import org.lin1473.shortlink.admin.toolkit.RandomGenerator;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 短链接分组接口实现层
 *
 */
@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

    /**
     * 后续重构为 SpringCloud Feign 调用
     */
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };

    @Override
    public void saveGroup(String groupName) {
        saveGroup(UserContext.getUsername(), groupName);
    }

    @Override
    public void saveGroup(String username, String groupName) {
        String gid ;
        do {
            gid = RandomGenerator.generateRandom();
        } while (!hasGid(username, gid));
        GroupDO groupDO = GroupDO.builder()
                .gid(gid)
                .sortOrder(0)
                .username(username)
                .name(groupName)
                .build();
        baseMapper.insert(groupDO);
    }

    @Override
    public List<ShortLinkGroupRespDTO> listGroup() {
        // 1. 查询分组列表
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag, 0)
                // TODO 从当前上下文获取用户名 DONE
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);
        List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);

        //2. 调用分组内短链接总量接口
        List<String> gidList = groupDOList.stream().map(GroupDO::getGid).toList();
        Result<List<ShortLinkGroupCountQueryRespDTO>> listResult = shortLinkRemoteService
                .listGroupShortLinkCount(gidList);

        // 3. 将 GroupDO 转成 ShortLinkGroupRespDTO
        List<ShortLinkGroupRespDTO> shortLinkGroupRespDTOList = BeanUtil.copyToList(groupDOList, ShortLinkGroupRespDTO.class);

        // 4. 用 Map 加速查找，保证 shortLinkCount 不为 null
        Map<String, Integer> countMap = Optional.ofNullable(listResult.getData()).orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        ShortLinkGroupCountQueryRespDTO::getGid,
                        dto -> dto.getShortLinkCount() == null ? 0 : dto.getShortLinkCount()
                ));

        //5. 给每个分组填充短链接数量
        shortLinkGroupRespDTOList.forEach(dto -> {
            dto.setShortLinkCount(countMap.getOrDefault(dto.getGid(), 0));
        });

        return shortLinkGroupRespDTOList;

        // 以下是课程的代码：
//        Result<List<ShortLinkGroupCountQueryRespDTO>> listResult = shortLinkRemoteService
//                .listGroupShortLinkCount(groupDOList.stream().map(GroupDO::getGid).toList());
//        List<ShortLinkGroupRespDTO> shortLinkGroupRespDTOList = BeanUtil.copyToList(groupDOList, ShortLinkGroupRespDTO.class);
//        shortLinkGroupRespDTOList.forEach(each -> {
//            Optional<ShortLinkGroupCountQueryRespDTO> first = listResult.getData().stream()
//                    .filter(item -> Objects.equals(item.getGid(), each.getGid()))
//                    .findFirst();
//            first.ifPresent(item -> each.setShortLinkCount(first.get().getShortLinkCount()));
//        });
//        return shortLinkGroupRespDTOList;
    }

    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, requestParam.getGid())
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = new GroupDO();
        groupDO.setName(requestParam.getName());
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void deleteGroup(String gid) {
        // 一般是软删除，修改flag
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = new GroupDO();
        groupDO.setDelFlag(1);
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void sortGroup(List<ShortLinkGroupSortReqDTO> requestParam) {
        requestParam.forEach(each -> {
            GroupDO groupDO = GroupDO.builder()
                    .sortOrder(each.getSortOrder())
                    .build();
            LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getUsername, UserContext.getUsername())
                    .eq(GroupDO::getGid, each.getGid())
                    .eq(GroupDO::getDelFlag, 0);
            baseMapper.update(groupDO, updateWrapper);
        });
    }

    private boolean hasGid(String username, String gid) {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getGid, gid)
                // TODO 设置用户名，网关传入  DONE
                .eq(GroupDO::getUsername, Optional.ofNullable(username).orElse(UserContext.getUsername()));
        GroupDO hasGroupFlag = baseMapper.selectOne(queryWrapper);
        return hasGroupFlag == null;
    }
}
