package org.lin1473.shortlink.admin.controller;


import lombok.RequiredArgsConstructor;
import org.lin1473.shortlink.admin.common.convention.result.Result;
import org.lin1473.shortlink.admin.common.convention.result.Results;
import org.lin1473.shortlink.admin.remote.ShortLinkRemoteService;
import org.lin1473.shortlink.admin.remote.dto.req.RecycleBinSaveReqDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 回收站管理控制层
 */
@RestController
@RequiredArgsConstructor
public class RecycleBinController {

    /**
     * TODO 后续重构为 SpringCloud Feign 调用
     */
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };

    /**
     * 保存回收站
     */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam) {
        shortLinkRemoteService.saveRecycleBin(requestParam);
        return Results.success();
    }
}
