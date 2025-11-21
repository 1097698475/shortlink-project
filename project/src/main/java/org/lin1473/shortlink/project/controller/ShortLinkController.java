package org.lin1473.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.lin1473.shortlink.project.common.convention.result.Result;
import org.lin1473.shortlink.project.common.convention.result.Results;
import org.lin1473.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.lin1473.shortlink.project.service.ShortLinkService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 短链接控制层
 */
@RestController
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    /**
     * 创建短链接
     */
    @PostMapping("/api/short-link/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam) {
        return Results.success(shortLinkService.createShortLink(requestParam));
    }

    @PutMapping("/api/short-link/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam) {
        shortLinkService.updateShortLink(requestParam);
        return Results.success();
    }

    /**
     * 分页查询短链接
     */
    @GetMapping("/api/short-link/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLinks(ShortLinkPageReqDTO requestParam) {
        return Results.success(shortLinkService.pageShortLink(requestParam));
    }

    /**
     * 查询短链接分组内短链接数量
     */
    @GetMapping("/api/short-link/v1/count")
    public Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(@RequestParam("requestParam") List<String> requestParam) {
        return Results.success(shortLinkService.listGroupShortLinkCount(requestParam));
    }
}
