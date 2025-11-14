package org.lin1473.shortlink.admin.controller;


import lombok.RequiredArgsConstructor;
import org.lin1473.shortlink.admin.common.convention.result.Result;
import org.lin1473.shortlink.admin.common.convention.result.Results;
import org.lin1473.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import org.lin1473.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import org.lin1473.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import org.lin1473.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import org.lin1473.shortlink.admin.service.GroupService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 短链接分组控制层
 */
@RestController
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;    //构造方法注入

    /**
     * 新增短链接分组
     */
    @PostMapping("/api/short-link/v1/group")
    public Result<Void> save(@RequestBody ShortLinkGroupSaveReqDTO RequestParam) {
        groupService.saveGroup(RequestParam.getName());
        return Results.success();
    }

    /**
     * 查询短链接分组集合
     */
    @GetMapping("/api/short-link/v1/group")
    public Result<List<ShortLinkGroupRespDTO>> listGroups() {
        return Results.success(groupService.listGroup());
    }

    /**
     * 修改短链接分组名称
     */
    @PutMapping("/api/short-link/v1/group")
    public Result<Void> update(@RequestBody ShortLinkGroupUpdateReqDTO RequestParam) {
        groupService.updateGroup(RequestParam);
        return Results.success();
    }

    /**
     * 删除短链接分组
     */
    @DeleteMapping("/api/short-link/v1/group")
    public Result<Void> updateGroup(@RequestParam String gid) {
        groupService.deleteGroup(gid);
        return Results.success();
    }

    /**
     * 排序短链接分组
     */
    @PostMapping("/api/short-link/v1/group/sort")
    public Result<Void> sortGroup(@RequestBody List<ShortLinkGroupSortReqDTO> RequestParam) {
        groupService.sortGroup(RequestParam);
        return Results.success();
    }
}
