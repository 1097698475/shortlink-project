package org.lin1473.shortlink.project.dto.req;

import lombok.Data;

/**
 * 获取分组内短链接监控数据 请求参数
 */
@Data
public class ShortLinkGroupStatsReqDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;
}
