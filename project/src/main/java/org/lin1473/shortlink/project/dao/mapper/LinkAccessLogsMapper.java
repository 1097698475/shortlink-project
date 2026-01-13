package org.lin1473.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.lin1473.shortlink.project.dao.entity.LinkAccessLogsDO;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsReqDTO;

import java.util.HashMap;
import java.util.List;

/**
 * 短链接访问 日志监控持久层
 */
public interface LinkAccessLogsMapper extends BaseMapper<LinkAccessLogsDO> {

    /**
     * 查询指定短链接在指定日期范围内的高频访问 IP（TOP N）
     *
     * SQL 功能说明：
     * - COUNT(ip)：统计每个 IP 的访问次数
     * - ORDER BY count DESC：按访问次数从高到低排序
     * - LIMIT 5：取访问频率最高的前 5 个 IP
     *
     * @param requestParam 请求参数：fullShortUrl、gid、startDate、endDate
     * @return 高频访问 IP 列表，映射成 List<HashMap<String, Object>>长这样：
     * [
     *     { "ip" : "1.1.1.1", "count" : "3" },
     *     { "ip" : "4.4.4.4", "count" : "2" },
     * ]
     */
    @Select("""
            SELECT
                ip,
                COUNT(ip) AS count
            FROM
                t_link_access_logs
            WHERE
                full_short_url = #{param.fullShortUrl}
                AND gid = #{param.gid}
                AND create_time BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY
                ip
            ORDER BY
                count DESC
            LIMIT 5
            """)
    List<HashMap<String, Object>> listTopIpByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 查询指定短链接在指定日期范围内的新老访客数量
     *
     * SQL 功能说明：
     * - 内层子查询：以 user 为维度进行分组（一个 user 代表一个 UV）
     *   - old_user：
     *       - 若该 user 在多个不同日期访问过（COUNT(DISTINCT DATE(create_time)) > 1）
     *       - 判定为老访客，记为 1
     *   - new_user：
     *       - 若该 user 只在一天访问过
     *       - 且最近一次访问时间落在指定日期范围内
     *       - 判定为新访客，记为 1
     * - 外层查询：
     *   - SUM(old_user)：统计老访客总数
     *   - SUM(new_user)：统计新访客总数
     *
     * 使用场景：
     * - 分析短链接的拉新能力
     * - 判断用户复访情况
     *
     * @param requestParam 请求参数：fullShortUrl、gid、startDate、endDate
     * @return 新老访客统计结果
     *         - oldUserCnt：老访客数量
     *         - newUserCnt：新访客数量
     *         映射成 List<HashMap<String, Object>>长这样：
     * [
     *     { "oldUserCnt" : 100, "newUserCnt" : 50 },
     * ]
     */
    @Select("""
            SELECT
                SUM(old_user) AS oldUserCnt,
                SUM(new_user) AS newUserCnt
            FROM (
                SELECT
                    CASE
                        WHEN COUNT(DISTINCT DATE(create_time)) > 1
                        THEN 1 ELSE 0
                    END AS old_user,
                    CASE
                        WHEN COUNT(DISTINCT DATE(create_time)) = 1
                             AND MAX(create_time) >= #{param.startDate}
                             AND MAX(create_time) <= #{param.endDate}
                        THEN 1 ELSE 0
                    END AS new_user
                FROM
                    t_link_access_logs
                WHERE
                    full_short_url = #{param.fullShortUrl}
                    AND gid = #{param.gid}
                GROUP BY
                    user
            ) AS user_counts
            """)
    HashMap<String, Object> findUvTypeCntByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);


}