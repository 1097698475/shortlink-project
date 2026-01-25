package org.lin1473.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.lin1473.shortlink.project.dao.entity.LinkAccessLogsDO;
import org.lin1473.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsReqDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                AND create_time >= #{param.startDate}
                AND create_time < DATE_ADD(#{param.endDate}, INTERVAL 1 DAY)
            GROUP BY
                ip
            ORDER BY
                count DESC
            LIMIT 5
            """)
    List<HashMap<String, Object>> listTopIpByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 查询分组在指定日期范围内的高频访问 IP（TOP N）
     *
     * SQL 功能说明：
     * - COUNT(ip)：统计每个 IP 的访问次数
     * - ORDER BY count DESC：按访问次数从高到低排序
     * - LIMIT 5：取访问频率最高的前 5 个 IP
     *
     * @param requestParam 请求参数：gid、startDate、endDate
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
                gid = #{param.gid}
                AND create_time >= #{param.startDate}
                AND create_time < DATE_ADD(#{param.endDate}, INTERVAL 1 DAY)
            GROUP BY
                ip
            ORDER BY
                count DESC
            LIMIT 5
            """)
    List<HashMap<String, Object>> listTopIpByGroup(@Param("param") ShortLinkGroupStatsReqDTO requestParam);

    /**
     * 查询指定短链接在指定日期范围内的新老访客数量
     * 新访客：在requestParam的 startDate 之前从未访问过，但在 [startDate, endDate] 内访问过
     * 老访客：在requestParam的 startDate 之前访问过，且在 [startDate, endDate] 内也访问过
     * 注意：这里访客数量是uv，根据uvcookie统计的，同一个用户多次访问，也只会统计1次新/老访客
     *
     * SQL 功能说明：
     * - 内层子查询：以 user 为维度进行分组（一个 user 代表一个 UV），MAX是聚合函数，一个user可能有多条记录，只要有一条记录满足CASE条件，MAX 就会将多条记录聚合为1
     * - 内层子查询的结果示例：
     *   user   has_before  has_in range
     *   u1     1           1
     *   u2     1           0
     *   u3     0           1
     *
     * - 外层查询：
     *   - SUM(old_user)：统计老访客总数
     *   - SUM(new_user)：统计新访客总数
     *
     * @param requestParam 请求参数：fullShortUrl、gid、startDate、endDate
     * @return 新老访客统计结果
     *         - oldUserCnt：老访客数量
     *         - newUserCnt：新访客数量
     *         映射成 List<HashMap<String, Object>>长这样：
     *         {
     *             "oldUserCnt": 10,
     *             "newUserCnt": 5
     *         }
     */
    @Select("""
            SELECT
                SUM(
                    CASE
                        WHEN has_before = 1 AND has_in_range = 1 THEN 1 ELSE 0
                    END
                ) AS oldUserCnt,
                SUM(
                    CASE
                        WHEN has_before = 0 AND has_in_range = 1 THEN 1 ELSE 0
                    END
                ) AS newUserCnt
            FROM (
                    SELECT
                        user,
                        MAX(CASE WHEN Date(create_time) < #{param.startDate} THEN 1 ELSE 0 END) AS has_before,
                        MAX(CASE WHEN Date(create_time) BETWEEN #{param.startDate} AND #{param.endDate} THEN 1 ELSE 0 END) AS has_in_range
                    FROM t_link_access_logs
                    WHERE full_short_url = #{param.fullShortUrl}
                      AND gid = #{param.gid}
                    GROUP BY user 
            ) AS user_counts    -- 这个 AS use_counts没有实际含义，是将内层sql查到的表起一个别名user_counts
            """)
    HashMap<String, Object> findUvTypeCntByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);


    /**
     * 查询指定短链接在指定日期 userList 是新访客/老访客
     * 由于传入的userList就已经在start和end之间访问过这个短链接，
     * 所以还需要根据userList查全表，判读每个user的第一次访问时间是否在指定日期内
     * 如果在，就是新用户，否则一定是老用户，无需判断第一次访问时间是否<startDate
     *
     * @param gid   分组标识
     * @param fullShortUrl  完整短链接
     * @param startDate     起始日期
     * @param endDate       结束日期
     * @param userList      去重的uvCookie列表
     * @return 返回格式如下：
     * {
     *     {"user": "u1", "uvType": "新访客" },
     *     {"user": "u2", "uvType": "老访客"},
     *     ...
     * }
     */
    @Select("""
            <script>
            SELECT
                user,
                CASE
                    WHEN MIN(DATE(create_time)) BETWEEN #{startDate} AND #{endDate}
                    THEN '新访客'
                    ELSE '老访客'
                END AS uvType
            FROM t_link_access_logs
            WHERE full_short_url = #{fullShortUrl}
                AND gid = #{gid}
                AND user IN
                <foreach collection="userList" item="item" open="(" separator="," close=")">
                    #{item}
                </foreach>
            GROUP BY user
            </script>
        """)
    List<Map<String, Object>> selectUvTypeByUsers(
            @Param("gid") String gid,
            @Param("fullShortUrl") String fullShortUrl,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("userList") List<String> userList
    );
}