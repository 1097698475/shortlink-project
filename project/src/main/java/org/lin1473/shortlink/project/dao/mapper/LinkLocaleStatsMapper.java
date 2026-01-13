package org.lin1473.shortlink.project.dao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.lin1473.shortlink.project.dao.entity.LinkLocaleStatsDO;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsReqDTO;

import java.util.List;

/**
 * 地区访问 统计持久层
 */
public interface LinkLocaleStatsMapper extends BaseMapper<LinkLocaleStatsDO> {

    /**
     * 写入短链接地区访问监控数据
     *
     * SQL 功能说明：
     * - 插入一条新的地区访问记录到 t_link_locale_stats
     * - 如果 full_short_url + gid + date + province + city + adcode 已存在，使用 ON DUPLICATE KEY UPDATE 做累加：
     *     - cnt = cnt + 新增 cnt
     * - create_time 和 update_time 使用 NOW() 自动生成时间
     *
     * @param linkLocaleStatsDO 短链接地区访问统计实体
     */
    @Insert("""
            INSERT INTO t_link_locale_stats (
                full_short_url, 
                gid, 
                date, 
                cnt, 
                country, 
                province, 
                city, 
                adcode, 
                create_time, 
                update_time, 
                del_flag
            ) VALUES (
                #{linkLocaleStats.fullShortUrl}, 
                #{linkLocaleStats.gid}, 
                #{linkLocaleStats.date}, 
                #{linkLocaleStats.cnt}, 
                #{linkLocaleStats.country}, 
                #{linkLocaleStats.province}, 
                #{linkLocaleStats.city}, 
                #{linkLocaleStats.adcode}, 
                NOW(), 
                NOW(), 
                0
            )
            ON DUPLICATE KEY UPDATE 
                cnt = cnt + #{linkLocaleStats.cnt}
            """)
    void shortLinkLocaleStats(@Param("linkLocaleStats") LinkLocaleStatsDO linkLocaleStatsDO);

    /**
     * 查询指定短链接在指定日期范围内的省份访问统计
     *
     * SQL 功能说明：
     * - SUM(cnt)：按省份对访问量累加，和pv一个意思
     * - GROUP BY province：按省份统计
     * - 用于分析短链接在各省份的访问分布
     *
     * @param requestParam 请求参数：fullShortUrl、gid、startDate、endDate
     * @return 每个省份访问量列表 由于只select了province和sum(cnt)，所以一个LinkLocaleStatsDO只有这两个字段有值，其他为null
     */
    @Select("""
            SELECT
                province,
                SUM(cnt) AS cnt
            FROM
                t_link_locale_stats
            WHERE
                full_short_url = #{param.fullShortUrl}
                AND gid = #{param.gid}
                AND date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY
                province
            """)
    List<LinkLocaleStatsDO> listLocaleByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

}
