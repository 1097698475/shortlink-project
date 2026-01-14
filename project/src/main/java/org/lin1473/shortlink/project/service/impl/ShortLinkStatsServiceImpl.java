package org.lin1473.shortlink.project.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import lombok.RequiredArgsConstructor;
import org.lin1473.shortlink.project.dao.entity.LinkBaseStatsDO;
import org.lin1473.shortlink.project.dao.entity.LinkDeviceStatsDO;
import org.lin1473.shortlink.project.dao.entity.LinkLocaleStatsDO;
import org.lin1473.shortlink.project.dao.entity.LinkNetworkStatsDO;
import org.lin1473.shortlink.project.dao.mapper.*;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.lin1473.shortlink.project.dto.resp.*;
import org.lin1473.shortlink.project.service.ShortLinkStatsService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 短链接监控接口实现层
 */
@Service
@RequiredArgsConstructor
public class ShortLinkStatsServiceImpl implements ShortLinkStatsService {

    private final LinkBaseStatsMapper linkBaseStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;

    @Override
    public ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam) {

        // 指定短链接在日期范围内的日粒度统计
        List<LinkBaseStatsDO> listDayStatsByShortLink = linkBaseStatsMapper.listDayStatsByShortLink(requestParam);
        if (CollUtil.isEmpty(listDayStatsByShortLink)) {
            return null;
        }
        //  注意：返回List是ShortLinkStatsReqDTO的开始日期到结束日期这之间的所有日期的pv uv uip，
        //  如果某一天没有访问数据，也要返回0，以便展示每天的折线图
        List<ShortLinkStatsAccessDailyRespDTO> daily = new ArrayList<>();   // oneShortLinkStats接口需要返回的列表DTO
        // 构造日期范围列表
        List<String> rangeDates = DateUtil.rangeToList(
                DateUtil.parse(requestParam.getStartDate()),
                DateUtil.parse(requestParam.getEndDate()),
                DateField.DAY_OF_MONTH
                ).stream()
                .map(DateUtil::formatDate)
                .toList();
        // 遍历每一天，找数据库返回的数据，没有就补 0
        rangeDates.forEach(each -> listDayStatsByShortLink.stream()
                .filter(item -> Objects.equals(each, DateUtil.formatDate(item.getDate())))
                .findFirst()
                .ifPresentOrElse(item -> {
                    ShortLinkStatsAccessDailyRespDTO accessDailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                            .date(DateUtil.parse(each))     // 每个each都是String，DTO里面是Date类型
                            .pv(item.getPv())
                            .uv(item.getUv())
                            .uip(item.getUip())
                            .build();
                    daily.add(accessDailyRespDTO);
                }, () -> {  // 没有就补0
                    ShortLinkStatsAccessDailyRespDTO accessDailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                            .date(DateUtil.parse(each))
                            .pv(0)
                            .uv(0)
                            .uip(0)
                            .build();
                    daily.add(accessDailyRespDTO);
                }));

        // 小时访问详情，根据小时进行分组
        List<Integer> hourStats = new ArrayList<>();    // oneShortLinkStats接口需要返回的列表DTO
        List<LinkBaseStatsDO> listHourStatsByShortLink = linkBaseStatsMapper.listHourStatsByShortLink(requestParam);    // select hour sum(pv) group by hour
        for (int i = 0; i < 24; i++) {
            AtomicInteger hour = new AtomicInteger(i);
            int hourCnt = listHourStatsByShortLink.stream()
                    .filter(each -> Objects.equals(each.getHour(), hour.get()))
                    .findFirst()
                    .map(LinkBaseStatsDO::getPv)
                    .orElse(0);
            hourStats.add(hourCnt);
        }

        // 一周访问详情，根据weekday进行分组
        List<Integer> weekdayStats = new ArrayList<>();     // oneShortLinkStats接口需要返回的列表DTO
        List<LinkBaseStatsDO> listWeekdayStatsByShortLink = linkBaseStatsMapper.listWeekdayStatsByShortLink(requestParam);  // select weekday SUM(pv) as pv
        for (int i = 1; i < 8; i++) {
            AtomicInteger weekday = new AtomicInteger(i);
            int weekdayCnt = listWeekdayStatsByShortLink.stream()
                    .filter(each -> Objects.equals(each.getWeekday(), weekday.get()))
                    .findFirst()
                    .map(LinkBaseStatsDO::getPv)
                    .orElse(0);
            weekdayStats.add(weekdayCnt);
        }

        // 高频访问IP详情 LinkAccessLogs表会记录每次访问的ip等信息，使用这个日志表计算前5频率的ip
        List<ShortLinkStatsTopIpRespDTO> topIpStats = new ArrayList<>();        // oneShortLinkStats接口需要返回的列表DTO
        List<HashMap<String, Object>> listTopIpByShortLink = linkAccessLogsMapper.listTopIpByShortLink(requestParam);   //select ip COUNT(ip) AS count，最后映射为Map
        listTopIpByShortLink.forEach(each -> {
            ShortLinkStatsTopIpRespDTO statsTopIpRespDTO = ShortLinkStatsTopIpRespDTO.builder()
                    .ip(each.get("ip").toString())
                    .cnt(Integer.parseInt(each.get("count").toString()))    // Integer格式
                    .build();
            topIpStats.add(statsTopIpRespDTO);
        });

        // 访客访问类型详情
        // 新访客：在requestParam的 startDate 之前从未访问过，但在 [startDate, endDate] 内访问过
        // 老访客：在requestParam的 startDate 之前访问过，且在 [startDate, endDate] 内也访问过
        List<ShortLinkStatsUvRespDTO> uvTypeStats = new ArrayList<>();      // oneShortLinkStats接口需要返回的列表DTO
        HashMap<String, Object> findUvTypeByShortLink = linkAccessLogsMapper.findUvTypeCntByShortLink(requestParam);
        int oldUserCnt = Integer.parseInt(
                Optional.ofNullable(findUvTypeByShortLink)
                        .map(each -> each.get("oldUserCnt"))
                        .map(Object::toString)  // findUvTypeByShortLink.get("oldUserCnt")返回的是Object类型，因为HashMap<String, Object>，所以转成toString()安全
                        .orElse("0")
        );
        int newUserCnt = Integer.parseInt(
                Optional.ofNullable(findUvTypeByShortLink)
                        .map(each -> each.get("newUserCnt"))
                        .map(Object::toString)
                        .orElse("0")
        );
        int uvSum = oldUserCnt + newUserCnt;
        double oldRatio = (double) oldUserCnt / uvSum;
        double actualOldRatio = Math.round(oldRatio * 100.0) / 100.0;
        double newRatio = (double) newUserCnt / uvSum;
        double actualNewRatio = Math.round(newRatio * 100.0) / 100.0;
        ShortLinkStatsUvRespDTO newUvRespDTO = ShortLinkStatsUvRespDTO.builder()
                .uvType("newUser")
                .cnt(newUserCnt)
                .ratio(actualNewRatio)
                .build();
        uvTypeStats.add(newUvRespDTO);
        ShortLinkStatsUvRespDTO oldUvRespDTO = ShortLinkStatsUvRespDTO.builder()
                .uvType("oldUser")
                .cnt(oldUserCnt)
                .ratio(actualOldRatio)
                .build();
        uvTypeStats.add(oldUvRespDTO);

        /******************************
         * 以下统计的写法都是一个模版：根据对应功能的字段进行分组group by，并select 功能字段和sum(cnt)，同时计算比率
         ******************************/

        // 地区访问详情（仅国内），根据省份进行分组
        List<ShortLinkStatsLocaleCNRespDTO> localeCnStats = new ArrayList<>();      // oneShortLinkStats接口需要返回的列表DTO
        List<LinkLocaleStatsDO> listedLocaleByShortLink = linkLocaleStatsMapper.listLocaleByShortLink(requestParam);    // select province SUM(cnt) AS cnt
        int localeCntSum = listedLocaleByShortLink.stream()
                .mapToInt(LinkLocaleStatsDO::getCnt)
                .sum();     // 总点击量
        listedLocaleByShortLink.forEach(each -> {
            double ratio = (double) each.getCnt() / localeCntSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsLocaleCNRespDTO localeCNRespDTO = ShortLinkStatsLocaleCNRespDTO.builder()
                    .cnt(each.getCnt())
                    .locale(each.getProvince())
                    .ratio(actualRatio)
                    .build();
            localeCnStats.add(localeCNRespDTO);
        });

        // 浏览器访问详情，本质上和地区访问详情一样，不过那个是在List<LinkLocaleStatsDO>操作，这个在HashMap操作
        List<ShortLinkStatsBrowserRespDTO> browserStats = new ArrayList<>();    // oneShortLinkStats接口需要返回的列表DTO
        List<HashMap<String, Object>> listBrowserStatsByShortLink = linkBrowserStatsMapper.listBrowserStatsByShortLink(requestParam);
        int browserSum = listBrowserStatsByShortLink.stream()
                .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                .sum();
        listBrowserStatsByShortLink.forEach(each -> {
            double ratio = (double) Integer.parseInt(each.get("count").toString()) / browserSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsBrowserRespDTO browserRespDTO = ShortLinkStatsBrowserRespDTO.builder()
                    .cnt(Integer.parseInt(each.get("count").toString()))
                    .browser(each.get("browser").toString())
                    .ratio(actualRatio)
                    .build();
            browserStats.add(browserRespDTO);
        });

        // 操作系统访问详情，和浏览器访问详情一样的写法
        List<ShortLinkStatsOsRespDTO> osStats = new ArrayList<>();      // oneShortLinkStats接口需要返回的列表DTO
        List<HashMap<String, Object>> listOsStatsByShortLink = linkOsStatsMapper.listOsStatsByShortLink(requestParam);
        int osSum = listOsStatsByShortLink.stream()
                .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                .sum();
        listOsStatsByShortLink.forEach(each -> {
            double ratio = (double) Integer.parseInt(each.get("count").toString()) / osSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsOsRespDTO osRespDTO = ShortLinkStatsOsRespDTO.builder()
                    .cnt(Integer.parseInt(each.get("count").toString()))
                    .os(each.get("os").toString())
                    .ratio(actualRatio)
                    .build();
            osStats.add(osRespDTO);
        });

        // 访问设备类型详情，和浏览器访问详情一样的写法
        List<ShortLinkStatsDeviceRespDTO> deviceStats = new ArrayList<>();  // oneShortLinkStats接口需要返回的列表DTO
        List<LinkDeviceStatsDO> listDeviceStatsByShortLink = linkDeviceStatsMapper.listDeviceStatsByShortLink(requestParam);
        int deviceSum = listDeviceStatsByShortLink.stream()
                .mapToInt(LinkDeviceStatsDO::getCnt)
                .sum();
        listDeviceStatsByShortLink.forEach(each -> {
            double ratio = (double) each.getCnt() / deviceSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsDeviceRespDTO deviceRespDTO = ShortLinkStatsDeviceRespDTO.builder()
                    .cnt(each.getCnt())
                    .device(each.getDevice())
                    .ratio(actualRatio)
                    .build();
            deviceStats.add(deviceRespDTO);
        });

        // 访问网络类型详情，和浏览器访问详情一样的写法
        List<ShortLinkStatsNetworkRespDTO> networkStats = new ArrayList<>();    // oneShortLinkStats接口需要返回的列表DTO
        List<LinkNetworkStatsDO> listNetworkStatsByShortLink = linkNetworkStatsMapper.listNetworkStatsByShortLink(requestParam);
        int networkSum = listNetworkStatsByShortLink.stream()
                .mapToInt(LinkNetworkStatsDO::getCnt)
                .sum();
        listNetworkStatsByShortLink.forEach(each -> {
            double ratio = (double) each.getCnt() / networkSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsNetworkRespDTO networkRespDTO = ShortLinkStatsNetworkRespDTO.builder()
                    .cnt(each.getCnt())
                    .network(each.getNetwork())
                    .ratio(actualRatio)
                    .build();
            networkStats.add(networkRespDTO);
        });

        // pv uv uip字段呢
        return ShortLinkStatsRespDTO.builder()
                .daily(daily)
                .localeCnStats(localeCnStats)
                .hourStats(hourStats)
                .topIpStats(topIpStats)
                .weekdayStats(weekdayStats)
                .browserStats(browserStats)
                .osStats(osStats)
                .uvTypeStats(uvTypeStats)
                .deviceStats(deviceStats)
                .networkStats(networkStats)
                .build();
    }
}

