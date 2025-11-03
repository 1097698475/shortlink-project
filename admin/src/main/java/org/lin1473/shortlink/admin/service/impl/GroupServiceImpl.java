package org.lin1473.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.lin1473.shortlink.admin.dao.entity.GroupDO;
import org.lin1473.shortlink.admin.dao.mapper.GroupMapper;
import org.lin1473.shortlink.admin.service.GroupService;
import org.springframework.stereotype.Service;

/**
 * 短链接分组接口实现层
 */
@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {
}
