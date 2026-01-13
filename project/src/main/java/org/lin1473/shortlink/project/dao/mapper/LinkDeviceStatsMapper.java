package org.lin1473.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.lin1473.shortlink.project.dao.entity.LinkDeviceStatsDO;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsReqDTO;

import java.util.List;

/**
 * 访问设备 统计持久层
 */
public interface LinkDeviceStatsMapper extends BaseMapper<LinkDeviceStatsDO> {

    /**
     * 写入短链接访问设备监控数据
     *
     * SQL 功能说明：
     * - 向 t_link_device_stats 表插入一条访问设备统计记录
     * - 统计维度为：full_short_url + gid + date + device
     * - 如果记录已存在，则使用 ON DUPLICATE KEY UPDATE 做累加：
     *     - cnt = cnt + 新增 cnt
     * - create_time、update_time 使用 NOW() 自动生成
     *
     * @param linkDeviceStatsDO 短链接访问设备统计实体
     */
    @Insert("""
        INSERT INTO t_link_device_stats (
            full_short_url,
            gid,
            date,
            cnt,
            device,
            create_time,
            update_time,
            del_flag
        )
        VALUES (
            #{linkDeviceStats.fullShortUrl},
            #{linkDeviceStats.gid},
            #{linkDeviceStats.date},
            #{linkDeviceStats.cnt},
            #{linkDeviceStats.device},
            NOW(),
            NOW(),
            0
        )
        ON DUPLICATE KEY UPDATE
            cnt = cnt + #{linkDeviceStats.cnt}
        """)
    void shortLinkDeviceStats(@Param("linkDeviceStats") LinkDeviceStatsDO linkDeviceStatsDO);

    /**
     * 查询指定短链接在指定日期范围内的访问设备统计
     *
     * SQL 功能说明：
     * - SUM(cnt)：对相同访问设备的访问次数进行累加
     * - GROUP BY device：按访问设备维度进行聚合
     * - 用于分析不同设备类型（如 PC / Mobile 等）的访问分布情况
     *
     * @param requestParam 请求参数：fullShortUrl、gid、startDate、endDate
     * @return 访问设备统计列表，每条记录包含：
     *         - device：访问设备类型
     *         - cnt：访问次数
     *         - 其他字段为null（返回的是DO实体）
     */
    @Select("""
        SELECT
            device,
            SUM(cnt) AS cnt
        FROM
            t_link_device_stats
        WHERE
            full_short_url = #{param.fullShortUrl}
            AND gid = #{param.gid}
            AND date BETWEEN #{param.startDate} AND #{param.endDate}
        GROUP BY
            device
        """)
    List<LinkDeviceStatsDO> listDeviceStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);


}

