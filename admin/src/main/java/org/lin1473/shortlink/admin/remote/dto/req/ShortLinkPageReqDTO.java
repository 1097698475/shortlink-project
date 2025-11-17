package org.lin1473.shortlink.admin.remote.dto.req;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 短链接分页请求参数
 * 根据分组id查询下面的短链接并分页展示
 */
@Data
public class ShortLinkPageReqDTO extends Page{

    /**
     * 分组标识
     */
    private String gid;
}