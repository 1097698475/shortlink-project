package org.lin1473.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.lin1473.shortlink.project.dao.entity.LinkStatsTodayDO;

/**
 * 短链接今日统计 持久层
 */
public interface LinkStatsTodayMapper extends BaseMapper<LinkStatsTodayDO> {

    /**
     * 记录短链接访问的今日统计数据
     * 数据库表有唯一索引：full_short_url, gid, date，
     * 当有短链接被访问时，新增数据，默认today_uv pv uip值为1，在ShortLinkServiceImpl.shortLinkStats新增
     * 当天该短链接超过一次被访问时，就update这三个字段，根据uvFirstFlag和uipFlag自增0或1
     */
    @Insert("""
            INSERT INTO t_link_stats_today (
                full_short_url, 
                gid,
                date,
                today_uv, 
                today_pv, 
                today_uip, 
                create_time, 
                update_time,
                del_flag
            )
            VALUES( 
                #{linkTodayStats.fullShortUrl},
                #{linkTodayStats.gid}, 
                #{linkTodayStats.date}, 
                #{linkTodayStats.todayUv}, 
                #{linkTodayStats.todayPv}, 
                #{linkTodayStats.todayUip}, 
                NOW(), 
                NOW(), 
                0
            )
            ON DUPLICATE KEY UPDATE 
                today_uv = today_uv +  #{linkTodayStats.todayUv}, 
                today_pv = today_pv +  #{linkTodayStats.todayPv},
                today_uip = today_uip +  #{linkTodayStats.todayUip};
            """)
    void shortLinkTodayStats(@Param("linkTodayStats") LinkStatsTodayDO linkStatsTodayDO);
}
