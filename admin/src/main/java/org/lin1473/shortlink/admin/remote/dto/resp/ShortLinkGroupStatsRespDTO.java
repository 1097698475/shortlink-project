package org.lin1473.shortlink.admin.remote.dto.resp;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 短链接监控统计 响应参数
 * 不需要访客类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkGroupStatsRespDTO {

    /**
     * 在指定日期内的访问量
     */
    private Integer pv;

    /**
     * 在指定日期内的独立访客数
     */
    private Integer uv;

    /**
     * 在指定日期内的独立IP数
     */
    private Integer uip;

    /**
     * 基础访问详情（日粒度）
     * 在这个时间段的每日pv uv uip，和上面三个不同，上面的是这个时间段总的数据
     * 注意：返回List是ShortLinkStatsReqDTO的开始日期到结束日期这之间的所有日期的pv uv uip，如果某一天没有访问数据，也要返回0，以便展示每天的折线图
     */
    private List<ShortLinkStatsAccessDailyRespDTO> daily;

    /**
     * 小时访问详情
     * 在这个时间段内，每个小时的访问量（一共有24个）
     */
    private List<Integer> hourStats;

    /**
     * 一周七天访问详情
     * 在这个时间段内，按照每周七天统计访问量，返回List大小为7
     */
    private List<Integer> weekdayStats;

    /**
     * 高频访问IP详情
     * 访问量前10的ip展示出来
     */
    private List<ShortLinkStatsTopIpRespDTO> topIpStats;

    /**
     * 地区访问详情（仅国内）
     * 统计全国哪些省份有访问，以及访问数（这里就不区分是不是独立uv ip了，只要跳转了就+1），和占比
     * 列表结构，每个对象有“点击量、省份、比率”
     */
    private List<ShortLinkStatsLocaleCNRespDTO> localeCnStats;

    /**
     * 浏览器访问详情
     * 浏览器是 chrome 还是 safari 还是 edege ，其他，记录访问量和占比
     */
    private List<ShortLinkStatsBrowserRespDTO> browserStats;

    /**
     * 操作系统访问详情
     * 同上
     */
    private List<ShortLinkStatsOsRespDTO> osStats;

    /**
     * 访问设备类型详情
     * 同浏览器
     */
    private List<ShortLinkStatsDeviceRespDTO> deviceStats;

    /**
     * 访问网络类型详情
     * 同上
     */
    private List<ShortLinkStatsNetworkRespDTO> networkStats;
}
