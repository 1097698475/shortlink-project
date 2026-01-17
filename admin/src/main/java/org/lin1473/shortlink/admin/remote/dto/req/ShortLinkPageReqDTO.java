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

    /**
     * 排序标识
     * 可以按照时间、今日/累计访问次数(pv)、今日/累计访问人数(uv)、今日/累计访问ip数(uip)排序
     */
    private String orderTag;
}