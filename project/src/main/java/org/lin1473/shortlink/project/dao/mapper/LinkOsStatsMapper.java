package org.lin1473.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.lin1473.shortlink.project.dao.entity.LinkOsStatsDO;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsReqDTO;

import java.util.HashMap;
import java.util.List;

/**
 * 操作系统访问 统计持久层
 */
public interface LinkOsStatsMapper extends BaseMapper<LinkOsStatsDO> {

    /**
     * 写入短链接操作系统访问监控数据
     *
     * SQL 功能说明：
     * - 向 t_link_os_stats 表插入一条操作系统访问统计记录
     * - 统计维度为：full_short_url + gid + date + os
     * - 如果记录已存在，则使用 ON DUPLICATE KEY UPDATE 做累加：
     *     - cnt = cnt + 新增 cnt
     * - create_time、update_time 使用 NOW() 自动生成
     *
     * @param linkOsStatsDO 短链接操作系统访问统计实体
     */
    @Insert("""
        INSERT INTO t_link_os_stats (
            full_short_url,
            gid,
            date,
            cnt,
            os,
            create_time,
            update_time,
            del_flag
        ) VALUES (
            #{linkOsStats.fullShortUrl},
            #{linkOsStats.gid},
            #{linkOsStats.date},
            #{linkOsStats.cnt},
            #{linkOsStats.os},
            NOW(),
            NOW(),
            0
        )
        ON DUPLICATE KEY UPDATE
            cnt = cnt + #{linkOsStats.cnt}
        """)
    void shortLinkOsStats(@Param("linkOsStats") LinkOsStatsDO linkOsStatsDO);

    /**
     * 查询指定短链接在指定日期范围内的操作系统访问统计
     *
     * SQL 功能说明：
     * - SUM(cnt)：对相同操作系统的访问次数进行累加
     *
     * @param requestParam 请求参数：fullShortUrl、gid、startDate、endDate
     * @return 操作系统访问统计列表，映射为哈希表每条记录包含：
     *         - os：操作系统名称
     *         - count：访问次数
     *         映射成 List<HashMap<String, Object>>长这样：
     *  [
     *   { "os": "Windows", "count": 120},
     *   { "os": "Mac", "count": 50}
     *  ]
     */
    @Select("""
        SELECT
            os,
            SUM(cnt) AS count
        FROM
            t_link_os_stats
        WHERE
            full_short_url = #{param.fullShortUrl}
            AND gid = #{param.gid}
            AND date BETWEEN #{param.startDate} AND #{param.endDate}
        GROUP BY
            os
        """)
    List<HashMap<String, Object>> listOsStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);


}