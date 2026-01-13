package org.lin1473.shortlink.project.service.impl;


import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.lin1473.shortlink.project.dao.entity.*;
import org.lin1473.shortlink.project.dao.mapper.*;
import org.lin1473.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.lin1473.shortlink.project.dto.resp.*;
import org.lin1473.shortlink.project.service.ShortLinkStatsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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
        // 指定短链接在日期范围内的日粒度统计，列表形式，短链接基础访问统计
        List<LinkBaseStatsDO> listDayStatsByShortLink = linkBaseStatsMapper.listDayStatsByShortLink(requestParam);

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
        // 看这个时间段访问该短链接的是新用户还是老用户，这个时间段第一次访问就是新用户，不是第一次访问就是老用户
        List<ShortLinkStatsUvRespDTO> uvTypeStats = new ArrayList<>();      // oneShortLinkStats接口需要返回的列表DTO
        HashMap<String, Object> findUvTypeByShortLink = linkAccessLogsMapper.findUvTypeCntByShortLink(requestParam);
        int oldUserCnt = Integer.parseInt(findUvTypeByShortLink.get("oldUserCnt").toString());
        int newUserCnt = Integer.parseInt(findUvTypeByShortLink.get("newUserCnt").toString());
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

        return ShortLinkStatsRespDTO.builder()
                .daily(BeanUtil.copyToList(listDayStatsByShortLink, ShortLinkStatsAccessDailyRespDTO.class))
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

