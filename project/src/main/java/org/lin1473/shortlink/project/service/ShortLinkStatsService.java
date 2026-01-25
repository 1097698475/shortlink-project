package org.lin1473.shortlink.project.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import org.lin1473.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkGroupStatsRespDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkStatsRespDTO;

/**
 * 短链接监控接口层
 */
public interface ShortLinkStatsService {

    /**
     * 获取单个短链接监控数据
     * 返回的数据，前端需要用图表表示统计数据
     *
     * @param requestParam 获取短链接监控数据入参
     * @return 短链接监控数据
     */
    ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam);

    /**
     * 获取指定日期内分组内的短链接监控统计数据
     * 以一个分组粒度进行统计，返回的不是list，而是分组内短链接的总和统计数据
     *
     * @param requestParam 获取分组内短链接监控数据入参
     * @return 分组内短链接监控数据
     */
    ShortLinkGroupStatsRespDTO groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam);

    /**
     * 获取单个短链接指定时间内的访问记录数据
     * 分页获取，前端展示单个短链接的访问记录列表
     *
     * @param requestParam 获取短链接监控访问记录数据入参
     * @return 访问记录监控数据
     */
    IPage<ShortLinkStatsAccessRecordRespDTO> oneShortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam);

}

