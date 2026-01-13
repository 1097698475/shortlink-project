package org.lin1473.shortlink.project.dao.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lin1473.shortlink.project.common.database.BaseDO;

import java.util.Date;

/**
 * 短链接基础访问 统计实体
 * 何为基础？就是一定需要这些数据，可以用当天24小时所有pv uv uip相加得到当天数据，也可以得到一周的数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_link_base_stats")
public class LinkBaseStatsDO extends BaseDO {

    /**
     * id
     */
    private Long id;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 日期 yy-mm-dd
     */
    private Date date;

    /**
     * 访问量
     */
    private Integer pv;

    /**
     * 独立访客数
     */
    private Integer uv;

    /**
     * 独立ip数
     */
    private Integer uip;

    /**
     * 小时
     */
    private Integer hour;

    /**
     * 星期
     */
    private Integer weekday;
}

