package org.lin1473.shortlink.project.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.lin1473.shortlink.project.common.convention.result.Result;
import org.lin1473.shortlink.project.common.convention.result.Results;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsReqDTO;
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
     * 访问单个短链接指定时间内的监控统计数据
     * 前端用图表展示
     */
    @GetMapping("/api/short-link/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO shortLinkStatsReqDTO) {
        return Results.success(shortLinkStatsService.oneShortLinkStats(shortLinkStatsReqDTO));
    }

    /**
     * 单个短链接指定时间内的访问记录数据
     * 前端用分页列表显示这条短链接的访问记录，本质就是把t_link_access_logs日志表的数据展示出来
     */
    @GetMapping("/api/short-link/v1/stats/access-record")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        return Results.success(shortLinkStatsService.shortLinkStatsAccessRecord(requestParam));
    }
}
