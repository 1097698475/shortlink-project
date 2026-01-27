package org.lin1473.shortlink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.lin1473.shortlink.project.dao.entity.LinkAccessLogsDO;

/**
 * 分组内短链接指定时间内的访问记录 请求参数
 * 为什么这里分页是用LinkAccessLogsDO模版？因为分页查询最终是落实到查询某个数据库表，所以这里的DO填入这个数据库表对应的实体DO。
 * 前端需要传入pageNO和pageSize，这是继承Page类的参数，而且必须写
 */
@Data
public class ShortLinkGroupStatsAccessRecordReqDTO extends Page<LinkAccessLogsDO> {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 开始日期
     * 为什么不用LocalDateTime？
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;

}
