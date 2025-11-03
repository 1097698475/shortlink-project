package org.lin1473.shortlink.admin.controller;


import lombok.RequiredArgsConstructor;
import org.lin1473.shortlink.admin.service.GroupService;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短链接分组控制层
 */
@RestController
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;    //构造方法注入

}
