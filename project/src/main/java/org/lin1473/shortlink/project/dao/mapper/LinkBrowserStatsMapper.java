package org.lin1473.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.lin1473.shortlink.project.dao.entity.LinkBrowserStatsDO;
import org.lin1473.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsReqDTO;

import java.util.HashMap;
import java.util.List;

/**
 * 浏览器访问 统计持久层
 */
public interface LinkBrowserStatsMapper extends BaseMapper<LinkBrowserStatsDO> {

    /**
     * 写入短链接浏览器访问监控数据
     *
     * SQL 功能说明：
     * - 向 t_link_browser_stats 表插入一条浏览器访问统计记录
     * - 统计维度为：full_short_url + gid + date + browser
     * - 如果记录已存在，则使用 ON DUPLICATE KEY UPDATE 做累加：
     *     - cnt = cnt + 新增 cnt
     * - create_time、update_time 使用 NOW() 自动填充
     *
     * @param linkBrowserStatsDO 短链接浏览器访问统计实体
     */
    @Insert("""
        INSERT INTO t_link_browser_stats (
            full_short_url,
            gid,
            date,
            cnt,
            browser,
            create_time,
            update_time,
            del_flag
        ) VALUES (
            #{linkBrowserStats.fullShortUrl},
            #{linkBrowserStats.gid},
            #{linkBrowserStats.date},
            #{linkBrowserStats.cnt},
            #{linkBrowserStats.browser},
            NOW(),
            NOW(),
            0
        )
        ON DUPLICATE KEY UPDATE
            cnt = cnt + #{linkBrowserStats.cnt}
        """)
    void shortLinkBrowserStats(@Param("linkBrowserStats") LinkBrowserStatsDO linkBrowserStatsDO);

    /**
     * 查询指定短链接在指定日期范围内的浏览器访问统计
     *
     * SQL 功能说明：
     * - SUM(cnt)：对相同浏览器的访问次数进行累加
     * - 用于分析不同浏览器的访问占比情况
     *
     * @param requestParam 请求参数：fullShortUrl、gid、startDate、endDate
     * @return 浏览器访问统计列表，每条记录包含：
     *         - browser：浏览器类型
     *         - count：访问次数
     *         映射成 List<HashMap<String, Object>>长这样：
     *  [
     *   { "browser": "chrome", "count": 120},
     *   { "browser": "safari", "count": 50}
     *  ]
     */
    @Select("""
        SELECT
            browser,
            SUM(cnt) AS count
        FROM
            t_link_browser_stats
        WHERE
            full_short_url = #{param.fullShortUrl}
            AND gid = #{param.gid}
            AND date BETWEEN #{param.startDate} AND #{param.endDate}
        GROUP BY
            browser
        """)
    List<HashMap<String, Object>> listBrowserStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 查询指定分组在指定日期范围内的浏览器访问统计
     *
     * SQL 功能说明：
     * - SUM(cnt)：对相同浏览器的访问次数进行累加
     * - 用于分析不同浏览器的访问占比情况
     *
     * @param requestParam 请求参数：gid、startDate、endDate
     * @return 浏览器访问统计列表，每条记录包含：
     *         - browser：浏览器类型
     *         - count：访问次数
     *         映射成 List<HashMap<String, Object>>：
     *  [
     *   { "browser": "chrome", "count": 120},
     *   { "browser": "safari", "count": 50}
     *  ]
     */
    @Select("""
        SELECT
            browser,
            SUM(cnt) AS count
        FROM
            t_link_browser_stats
        WHERE
            gid = #{param.gid}
            AND date BETWEEN #{param.startDate} AND #{param.endDate}
        GROUP BY
            browser
        """)
    List<HashMap<String, Object>> listBrowserStatsByGroup(@Param("param") ShortLinkGroupStatsReqDTO requestParam);


}