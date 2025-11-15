package org.lin1473.shortlink.project.controller;

import lombok.RequiredArgsConstructor;
import org.lin1473.shortlink.project.common.convention.result.Result;
import org.lin1473.shortlink.project.common.convention.result.Results;
import org.lin1473.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import org.lin1473.shortlink.project.service.ShortLinkService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
