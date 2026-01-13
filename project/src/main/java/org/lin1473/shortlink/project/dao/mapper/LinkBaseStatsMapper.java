package org.lin1473.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.lin1473.shortlink.project.dao.entity.LinkBaseStatsDO;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsReqDTO;

import java.util.List;

/**
 * 短链接基础访问 统计持久层
 *
 */
public interface LinkBaseStatsMapper extends BaseMapper<LinkBaseStatsDO> {

    /**
     * 写入短链接基础访问监控数据
     *
     * SQL 功能说明：
     * - 插入一条新的访问记录到 t_link_access_stats。
     * - 如果 full_short_url + gid + date 已存在，使用 ON DUPLICATE KEY UPDATE 做累加：
     *     - pv = pv + 新增 pv
     *     - uv = uv + 新增 uv
     *     - uip = uip + 新增 uip
     * - create_time 和 update_time 使用 NOW() 自动生成时间
     *
     * @param linkBaseStatsDO 短链接基础访问 统计实体
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
    void shortLinkBaseStats(@Param("linkBaseStats") LinkBaseStatsDO linkBaseStatsDO);

    /**
     * 查询指定短链接在日期范围内的日粒度统计
     *
     * SQL 功能说明：
     * - SUM(pv/uv/uip)：对同一天的数据累加
     * - GROUP BY date：按日期聚合（每天）
     * - 这样可以得到这个gid的短链接的每天总的访问量（PV）、独立访客数（UV）、独立 IP 数（UIP）
     *
     * @param requestParam 请求参数：fullShortUrl、gid、startDate、endDate
     * @return 每天的访问统计列表，由于只select了date和sum(pv)等，所以一个LinkBaseStatsDO只有这四个字段有值，其他为null
     */
    @Select("""
            SELECT
                date,
                SUM(pv) AS pv,
                SUM(uv) AS uv,
                SUM(uip) AS uip
            FROM
                t_link_base_stats
            WHERE
                full_short_url = #{param.fullShortUrl}
                AND gid = #{param.gid}
                AND date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY
                date;   -- 这样写最简洁，教程是full_short_url, gid, hour，前两个字段都冗余了
            """)
    List<LinkBaseStatsDO> listDayStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 查询指定短链接在日期范围内的小时粒度统计
     *
     * SQL 功能说明：
     * - SUM(pv)：对每小时的数据累加
     * - GROUP BY hour：按小时聚合
     * - 用于分析每天每小时的访问量变化趋势
     *
     * @param requestParam 请求参数
     * @return 每小时访问量列表，由于只select了hour和sum(pv)，所以一个LinkBaseStatsDO只有这2个字段有值，其他为null
     */
    @Select("""
            SELECT
                hour,
                SUM(pv) AS pv
            FROM
                t_link_base_stats
            WHERE
                full_short_url = #{param.fullShortUrl}
                AND gid = #{param.gid}
                AND date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY
                hour;
            """)
    List<LinkBaseStatsDO> listHourStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 查询指定短链接在日期范围内的星期粒度统计
     *
     * SQL 功能说明：
     * - SUM(pv)：对同一天的访问量累加到对应的星期几
     * - GROUP BY weekday：按星期聚合
     * - 用于分析一周内访问量的分布趋势
     *
     * @param requestParam 请求参数
     * @return 每星期访问量列表，由于只select了weekday和sum(pv)，所以一个LinkBaseStatsDO只有这2个字段有值，其他为null
     */
    @Select("""
            SELECT
                weekday,
                SUM(pv) AS pv
            FROM
                t_link_base_stats
            WHERE
                full_short_url = #{param.fullShortUrl}
                AND gid = #{param.gid}
                AND date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY
                weekday;
            """)
    List<LinkBaseStatsDO> listWeekdayStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

}
