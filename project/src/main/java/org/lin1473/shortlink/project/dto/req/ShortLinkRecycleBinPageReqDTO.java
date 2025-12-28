package org.lin1473.shortlink.project.dto.req;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.lin1473.shortlink.project.dao.entity.ShortLinkDO;

import java.util.List;

/**
 * 回收站短链接分页请求参数
 * 不同于短链接分组查询直接传入一个gid，而是需要传入gid集合（用户的所有gid）
 * 根据分组id查询下面的短链接并分页展示
 */
@Data
public class ShortLinkRecycleBinPageReqDTO extends Page<ShortLinkDO>{

    /**
     * 分组标识
     */
    private List<String> gidList;
}