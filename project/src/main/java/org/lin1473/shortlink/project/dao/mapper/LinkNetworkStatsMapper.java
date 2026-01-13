package org.lin1473.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.lin1473.shortlink.project.dao.entity.LinkNetworkStatsDO;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsReqDTO;

import java.util.List;

/**
 * 访问网络 统计持久层
 */
public interface LinkNetworkStatsMapper extends BaseMapper<LinkNetworkStatsDO> {

    /**
     * 写入短链接访问网络环境监控数据
     *
     * SQL 功能说明：
     * - 向 t_link_network_stats 表插入一条访问网络统计记录
     * - 统计维度为：full_short_url + gid + date + network
     * - 如果记录已存在，则使用 ON DUPLICATE KEY UPDATE 做累加：
     *     - cnt = cnt + 新增 cnt
     * - create_time、update_time 使用 NOW() 自动生成
     *
     * @param linkNetworkStatsDO 短链接访问网络统计实体
     */
    @Insert("""
        INSERT INTO t_link_network_stats (
            full_short_url,
            gid,
            date,
            cnt,
            network,
            create_time,
            update_time,
            del_flag
        )
        VALUES (
            #{linkNetworkStats.fullShortUrl},
            #{linkNetworkStats.gid},
            #{linkNetworkStats.date},
            #{linkNetworkStats.cnt},
            #{linkNetworkStats.network},
            NOW(),
            NOW(),
            0
        )
        ON DUPLICATE KEY UPDATE
            cnt = cnt + #{linkNetworkStats.cnt}
        """)
    void shortLinkNetworkStats(@Param("linkNetworkStats") LinkNetworkStatsDO linkNetworkStatsDO);

    /**
     * 查询指定短链接在指定日期范围内的访问网络环境统计
     *
     * SQL 功能说明：
     * - SUM(cnt)：对相同网络环境的访问次数进行累加
     * - GROUP BY network：按网络类型维度进行聚合
     * - 常用于分析 WiFi、4G、5G 等网络环境的访问占比
     *
     * @param requestParam 请求参数：fullShortUrl、gid、startDate、endDate
     * @return 访问网络环境统计列表，每条记录包含：
     *         - network：网络类型
     *         - cnt：访问次数
     *         - 其他字段为null（返回的是DO实体）
     */
    @Select("""
        SELECT
            network,
            SUM(cnt) AS cnt
        FROM
            t_link_network_stats
        WHERE
            full_short_url = #{param.fullShortUrl}
            AND gid = #{param.gid}
            AND date BETWEEN #{param.startDate} AND #{param.endDate}
        GROUP BY
            network
        """)
    List<LinkNetworkStatsDO> listNetworkStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

}
