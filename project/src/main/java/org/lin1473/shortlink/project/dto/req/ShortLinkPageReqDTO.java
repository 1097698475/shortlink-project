package org.lin1473.shortlink.project.dto.req;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.lin1473.shortlink.project.dao.entity.ShortLinkDO;

/**
 * 短链接分页请求参数
 * 根据分组id查询下面的短链接并分页展示
 */
@Data
public class ShortLinkPageReqDTO extends Page<ShortLinkDO> {

    /**
     * 分组标识
     */
    private String gid;
}