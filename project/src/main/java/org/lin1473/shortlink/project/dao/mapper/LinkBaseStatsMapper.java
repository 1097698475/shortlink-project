package org.lin1473.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.lin1473.shortlink.project.dao.entity.LinkBaseStatsDO;

/**
 * 短链接基础访问持久层
 */
public interface LinkBaseStatsMapper extends BaseMapper<LinkBaseStatsDO> {

    /**
     * 记录基础访问监控数据
     */
    @Insert("""
            INSERT INTO t_link_base_stats (
                full_short_url,
                gid,
                date,
                pv,
                uv,
                uip,
                hour,
                weekday,
                create_time,
                update_time,
                del_flag
            )
            VALUES (
                #{linkBaseStats.fullShortUrl},
                #{linkBaseStats.gid},
                #{linkBaseStats.date},
                #{linkBaseStats.pv},
                #{linkBaseStats.uv},
                #{linkBaseStats.uip},
                #{linkBaseStats.hour},
                #{linkBaseStats.weekday},
                NOW(),
                NOW(),
                0
            )
            ON DUPLICATE KEY UPDATE
                pv = pv + #{linkBaseStats.pv},
                uv = uv + #{linkBaseStats.uv},
                uip = uip + #{linkBaseStats.uip};
            """)
    void shortLinkStats(@Param("linkBaseStats") LinkBaseStatsDO linkBaseStatsDO);

}
