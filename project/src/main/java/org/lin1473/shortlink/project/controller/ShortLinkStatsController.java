package org.lin1473.shortlink.project.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.lin1473.shortlink.project.common.convention.result.Result;
import org.lin1473.shortlink.project.common.convention.result.Results;
import org.lin1473.shortlink.project.dto.req.ShortLinkGroupStatsAccessRecordReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkGroupStatsRespDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkStatsRespDTO;
import org.lin1473.shortlink.project.service.ShortLinkStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短链接监控统计 控制层
 */
@RestController
@RequiredArgsConstructor
public class ShortLinkStatsController {

    private final ShortLinkStatsService shortLinkStatsService;

    /**
     * 访问单个短链接指定日期内的监控统计数据
     * 前端用图表展示
     */
    @GetMapping("/api/short-link/v1/stats")
    public Result<ShortLinkStatsRespDTO> oneShortLinkStats(ShortLinkStatsReqDTO shortLinkStatsReqDTO) {
        return Results.success(shortLinkStatsService.oneShortLinkStats(shortLinkStatsReqDTO));
    }

    /**
     * 获取指定日期内分组内的短链接监控统计数据
     * 以一个分组粒度进行统计，返回的不是list，而是分组内短链接的总和统计数据
     */
    @GetMapping("/api/short-link/v1/stats/group")
    public Result<ShortLinkGroupStatsRespDTO> groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        return Results.success(shortLinkStatsService.groupShortLinkStats(requestParam));
    }

    /**
     * 单个短链接指定时间内的访问记录数据
     * 前端用分页列表显示这条短链接的访问记录，本质就是把t_link_access_logs日志表的数据展示出来
     */
    @GetMapping("/api/short-link/v1/stats/access-record")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> oneShortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        return Results.success(shortLinkStatsService.oneShortLinkStatsAccessRecord(requestParam));
    }

    /**
     * 分组内所有短链接指定时间内的访问记录数据
     * 前端用分页列表显示这些短链接的访问记录，本质就是把t_link_access_logs日志表的数据展示出来
     */
    @GetMapping("/api/short-link/v1/stats/access-record/group")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        return Results.success(shortLinkStatsService.groupShortLinkStatsAccessRecord(requestParam));
    }
}
