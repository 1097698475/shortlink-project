package org.lin1473.shortlink.admin.controller;


import lombok.RequiredArgsConstructor;
import org.lin1473.shortlink.admin.common.convention.result.Result;
import org.lin1473.shortlink.admin.common.convention.result.Results;
import org.lin1473.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import org.lin1473.shortlink.admin.service.GroupService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短链接分组控制层
 */
@RestController
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;    //构造方法注入

    @PostMapping("/api/short-link/v1/group")
    public Result<Void> save(@RequestBody ShortLinkGroupSaveReqDTO RequestParam) {
        groupService.saveGroup(RequestParam.getName());
        return Results.success();
    }

}
