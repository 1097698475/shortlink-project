package org.lin1473.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.lin1473.shortlink.project.common.convention.exception.ClientException;
import org.lin1473.shortlink.project.common.convention.exception.ServiceException;
import org.lin1473.shortlink.project.common.enums.VailDateTypeEnum;
import org.lin1473.shortlink.project.dao.entity.*;
import org.lin1473.shortlink.project.dao.mapper.*;
import org.lin1473.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.lin1473.shortlink.project.service.ShortLinkService;
import org.lin1473.shortlink.project.toolkit.HashUtil;
import org.lin1473.shortlink.project.toolkit.LinkUtil;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.lin1473.shortlink.project.common.constant.RedisKeyConstant.*;
import static org.lin1473.shortlink.project.common.constant.ShortLinkConstant.IP2LOCATION_REMOTE_URL;

/**
 * 短链接接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final LinkBaseStatsMapper linkBaseStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;




    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        // 生成后缀
        String shortLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl = StrBuilder.create(requestParam.getDomain())
                .append("/")
                .append(shortLinkSuffix)
                .toString();
        // 填补ShortLinkDO - ShortLinkCreateReqDTO的字段
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(requestParam.getDomain())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .shortUri(shortLinkSuffix)
                .fullShortUrl(fullShortUrl)
                .enableStatus(0)
                .favicon(getFavicon(requestParam.getOriginUrl()))
                .totalPv(0)
                .totalUv(0)
                .totalUip(0)
                .build();
        ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder()
                .fullShortUrl(fullShortUrl)
                .Gid(requestParam.getGid())
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(shortLinkGotoDO);
        } catch (DuplicateKeyException ex) {
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getShortUri, fullShortUrl);
            ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
            if (hasShortLinkDO != null) {
                log.warn("短链接：{} 重复入库", fullShortUrl);
                throw new ServiceException("短链接生成重复");
            }
        }
        // 缓存预热 key value timeout
        stringRedisTemplate.opsForValue().set(
                String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                requestParam.getOriginUrl(),
                LinkUtil.getLinkCacheValidTime(requestParam.getValidDate()), TimeUnit.MILLISECONDS
                );
        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);
        // 链式创建
        return  ShortLinkCreateRespDTO.builder()
                .fullShortUrl("http://" + shortLinkDO.getFullShortUrl())    // 测试阶段直接拼http
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())       // 使用原始的分组标识查询（分片键路由到t_link_%d）
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())   // 完整短链接也要查询
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);    // fullShortUrl唯一索引，最多一条记录

        // 查询需要update的数据库记录，正常来说一定查到一条记录
        // TODO 疑问，使用的是前端传来可能修改过的gid，如果是修改的gid肯定查不到
        if (hasShortLinkDO == null) {
            throw new ClientException("短链接记录不存在");
        }

        // 判断gid是否发生变化：
        // 如果未变化，使用 UPDATE 更新原记录，如果有效期类型修改为永久，则validDate 字段设为 null；
        // 如果有变化，在旧 gid 下删除原记录，重新新增一条记录到新 gid（insert）
        if (Objects.equals(hasShortLinkDO.getGid(), requestParam.getGid())) {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid()) // Gid没有修改，originGid==Gid
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);   // 条件判断

            // 构造一个新的对象
            ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                    .domain(hasShortLinkDO.getDomain()) // 保留不可变字段，从数据库已有的记录hasShortLinkDO获取
                    .shortUri(hasShortLinkDO.getShortUri())
                    .clickNum(hasShortLinkDO.getClickNum())
                    .favicon(hasShortLinkDO.getFavicon())
                    .createdType(hasShortLinkDO.getCreatedType())
                    .gid(requestParam.getGid()) // Gid没有修改，originGid==Gid，写哪个都可以
                    .originUrl(requestParam.getOriginUrl())
                    .describe(requestParam.getDescribe())
                    .validDateType(requestParam.getValidDateType())
                    .validDate(requestParam.getValidDate())
                    .build();
            baseMapper.update(shortLinkDO, updateWrapper);
        } else {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getOriginGid())   // Gid已经修改，删除的是原始Gid对应的记录
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            baseMapper.delete(updateWrapper);

            // 构造一个新的对象
            ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                    .domain(hasShortLinkDO.getDomain()) // 保留不可变字段，从数据库已有的记录hasShortLinkDO获取
                    .shortUri(hasShortLinkDO.getShortUri())
                    .clickNum(hasShortLinkDO.getClickNum())
                    .favicon(hasShortLinkDO.getFavicon())
                    .createdType(hasShortLinkDO.getCreatedType())
                    .enableStatus(hasShortLinkDO.getEnableStatus())
                    .gid(requestParam.getGid()) // 覆盖可变字段，用前端传来的requestparam，使用修改的gid
                    .originUrl(requestParam.getOriginUrl())
                    .fullShortUrl(hasShortLinkDO.getFullShortUrl())   // TODO fullShortUrl是唯一索引，这里不能简单复制requestParam的，如果修改了fullShortUrl就需要判断是否不唯一
                    .describe(requestParam.getDescribe())
                    .validDateType(requestParam.getValidDateType())
                    .validDate(requestParam.getValidDate())
                    .build();
            baseMapper.insert(shortLinkDO);
        }
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        IPage<ShortLinkDO> resultPage = shortLinkMapper.selectPageLink(requestParam);
        return resultPage.convert(each -> {
            ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);    // 查询到的该分组标识gid下的t_link记录
            result.setDomain("http://" + result.getDomain());   // 单独设置domain字段，将协议类型加进去
            return result;
        });
    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid, count(*) as shortLinkCount")
                .in("gid", requestParam)
                .eq("enable_status", 0)
                .groupBy("gid");
        List<Map<String, Object>> shortLinkDOList = baseMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(shortLinkDOList, ShortLinkGroupCountQueryRespDTO.class);
    }

    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        // 测试用的：在浏览器输入nurl.ink:8001/1NJnCN， 就会跳转到nageoffer.com，nurl.ink:8001/1NJnCN会被解析成127.0.0.1:8001/1NJnCN，就是后管dev环境了
        String serverName = request.getServerName();    // 得到http://nurl.ink 相当于域名
        String fullShortUrl = serverName + "/" + shortUri;  // 和短链接uri拼接

        // 根据key查找redis对应的原始网址，如果查得到就不用查数据库。String.format就是将它们拼接起来
        String originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(originalLink)) {
            // 短链接跳转的时候，自增pv访问次数
            shortLinkStats(fullShortUrl, null, request, response);
            ((HttpServletResponse) response).sendRedirect(originalLink);
            return;
        }

        // 缓存查不到。先查询布隆过滤器，如果不存在直接返回404页面，如果存在，需要处理误判的情况：
        // 查询当前短链接是否在Redis有空值标记（不是真的空值，而是一个key标记），如果有空值标记说明数据库不存在原始链接（当前的请求是恶意请求），返回404页面；如果没有标记则查询数据库接下面操作。（查询数据库如果不存在，则缓存空值标记）
        boolean contains = shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl);
        if (!contains) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");    // 本质是跳转到127.0.0.1:8001/page/notfound
            return;
        }
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(gotoIsNullShortLink)) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");    // 本质是跳转到127.0.0.1:8001/page/notfound
            return;
        }

        // 查不到redis，直接查数据库，避免缓存击穿（大量请求打到数据库），需要设置分布式锁
        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();
        try {   // 为什么要try？因为代码中都可能会抛异常（如.opsForValue().set(...)， baseMapper.selectOne），一旦抛异常，如果不用try finally，就不会执行unlock，死锁
            // 锁内二次检查。假设每秒 1000 个请求进来，全部尝试去执行 lock.lock()，只有一个线程成功获得锁，其他线程阻塞等待，获得锁的线程查询数据库，并把结果写到 Redis，其他线程随后也会获得锁（依次排队），如果锁内不检查第二次 Redis，那么所有线程都要查数据库
            originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
            if (StrUtil.isNotBlank(originalLink)) {
                // 短链接跳转的时候，自增pv访问次数
                shortLinkStats(fullShortUrl, null, request, response);
                ((HttpServletResponse) response).sendRedirect(originalLink);
                return;
            }

            // 先查ShortLinkGoto表，得到gid
            LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
            if (shortLinkGotoDO == null) {
                // 严谨来说此处需要进行封控
                // 查询数据库如果不存在，则缓存空值标记，返回404页面。下次这样的请求就直接在redis判断空标记，返回，而不用查数据库，避免大量恶意请求穿透数据库
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");    // 本质是跳转到127.0.0.1:8001/page/notfound
                return;
            }

            // 根据gid，再查ShortLink表
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);

            // 将数据库查到的记录写入redis
            // 如果没查到，或者过期，就空值标记，返回404页面，不过期再添加原始链接到redis
            if (shortLinkDO == null || (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date()))) {
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");    // 本质是跳转到127.0.0.1:8001/page/notfound
                return;
            }
            // 如果查到了，写入redis，并直接跳转原始网站
            stringRedisTemplate.opsForValue().set(
                    String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                    shortLinkDO.getOriginUrl(),
                    LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()), TimeUnit.MILLISECONDS
            );
            // 短链接跳转的时候，自增pv访问次数
            shortLinkStats(fullShortUrl, shortLinkDO.getGid(), request, response);
            ((HttpServletResponse) response).sendRedirect(shortLinkDO.getOriginUrl());  //重定向
        } finally {
            lock.unlock();
        }
    }

    /**
     * 创建/更新
     * TODO 这个方法会重构到ShortLinkStatsServiceImpl里面的oneShortLinkStats方法s
     * @param fullShortUrl 完整短链接
     * @param gid 分组标识
     * @param request 浏览器请求
     * @param response 浏览器响应
     */
    private void shortLinkStats(String fullShortUrl, String gid, ServletRequest request, ServletResponse response) {
        AtomicBoolean uvFirstFlag = new AtomicBoolean();    // 判断是否第一次访问，第一次访问会创建cookie，同时用户访问量uv++
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();     // 浏览器的http请求，可能会保存cookie
        try {
            AtomicReference<String> uv = new AtomicReference<>();   // 普通变量不能在匿名函数里面赋值修改，这种final变量才可以
            // 匿名函数，不调用不会执行
            Runnable addResponseCookieTask = () -> {
                // 使用cookie 标识同一个用户
                uv.set(UUID.fastUUID().toString());     // 赋值final变量，以便外部的访问日志对象能使用
                Cookie uvCookie = new Cookie("uv", uv.get());
                uvCookie.setMaxAge(60 * 60 * 24 * 30);   // 30天有效期
                uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));  // 如果full是http://nurl.ink/3draim， 那么切割为 /3draim
                ((HttpServletResponse) response).addCookie(uvCookie);   // 方法全部执行完后，Spring内置的服务器会给浏览器发送该响应
                uvFirstFlag.set(Boolean.TRUE);
                stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, uv.get());   // 这里的结构是key=“short-link:stats:uv:xxx”, value={"uv1","uv2"...}，value是个集合，不会重复
            };
            // 先看处理请求里面有没有cookies：
            // 如果有cookie，直接缓存，表明当前请求是同一个用户，uv不用+1，uvFirstFlag.get()为false
            // 如果没有cookie，就需要先创建，uv需要+1，uvFirstFlag.get()为true
            if (ArrayUtil.isNotEmpty(cookies)) {
                Arrays.stream(cookies)
                        .filter(each -> Objects.equals(each.getName(), "uv"))
                        .findFirst()
                        .map(Cookie::getValue)
                        // 这一段的逻辑是：找到了 uvCookie，就尝试加入redis集合，如果是第一次加入，added=1, 此时uvFirstFlag=True；如果已经加入过，added=0，flag=false
                        // 没有找到 uvCookie， 就执行addResponseCookieTask（该代码段中，也需要加入redis，设置uvFirstFlag=true)
                        .ifPresentOrElse(each -> {  // 有cookie
                            uv.set(each);   // 赋值final变量，以便外部的访问日志对象能使用
                            Long uvAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, each);
                            uvFirstFlag.set(uvAdded != null && uvAdded > 0L);
                        }, addResponseCookieTask);
            } else {
                // 没有cookie时：
                addResponseCookieTask.run();
            }
            // 统计UIP，和uv差不多，ip相同的请求，多次访问，uip不会变。尝试添加redis，第一次添加 added=1，此时flag=true；否则added=0, flag=false
            String remoteAddr = LinkUtil.getActualIp((HttpServletRequest) request);     // 单独抽出来变量，AccessLogs日志表需要
            Long uipAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uip:" + fullShortUrl, remoteAddr);
            boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;

            // gid可能会不传入
            if (StrUtil.isBlank(gid)) {
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                gid = shortLinkGotoDO.getGid();
            }
            // 获取当前时间是第几小时（24小时制）   new Date()是当前时间，精确到秒
            int hour = DateUtil.hour(new Date(), true);     // 0-23
            // 获取当前时间是周几
            Week week = DateUtil.dayOfWeekEnum(new Date()); //1-7
            int weekValue = week.getIso8601Value();

            // 使用builder创建DO对象
            LinkBaseStatsDO linkBaseStatsDO = LinkBaseStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())   // yy-mm-dd
                    .pv(1)       // 在linkAccessStatsMapper里面，pv = pv +  #{linkAccessStats.pv}， 这里=1则表示自增1
                    .uv(uvFirstFlag.get() ? 1 : 0)   // 在linkAccessStatsMapper里面，uv = uv + #{linkAccessStats.uv}，如果=0就维持uv不变
                    .uip(uipFirstFlag ? 1 : 0)
                    .hour(hour)
                    .weekday(weekValue)
                    .build();
            linkBaseStatsMapper.shortLinkBaseStats(linkBaseStatsDO);    // 调用自定义的SQL操作，因为mybatis做不了

            // 调用ip2location IP查询接口
            Map<String, Object> localeParamMap = new HashMap<>();
            localeParamMap.put("ip", remoteAddr);   // 接口只有一个参数
            String localeResultStr = HttpUtil.get(IP2LOCATION_REMOTE_URL, localeParamMap);  // 发起GET请求，自动拼接成 https://api.ip2location.io/?ip=xxx
            JSONObject localeResultObj = JSON.parseObject(localeResultStr);
            String actualProvince = null;  // 单独抽出来变量，AccessLogs日志表需要
            String actualCity = null;      // 单独抽出来变量，AccessLogs日志表需要
            // 如果请求成功
            String country = localeResultObj.getString("country_name"); // 请求失败不会有country_name字段
            if (StrUtil.isNotBlank(country) || "127.0.0.1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
                boolean unknownFlag = StrUtil.equals(country, "null") || country == null;
                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                        .country("中国")  // 只做国内
                        .province(actualProvince = unknownFlag ? "未知" : localeResultObj.getString("region_name"))
                        .city(actualCity = unknownFlag ? "未知" : localeResultObj.getString("city_name"))
                        .adcode(unknownFlag ? "未知" : localeResultObj.getString("zip_code"))   // 其实zip_code是邮编，adcode是城市编码，不一样
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .gid(gid)
                        .date(new Date())
                        .build();
                linkLocaleStatsMapper.shortLinkLocaleStats(linkLocaleStatsDO); // 调用自定义的SQL操作，因为mybatis做不了
            }

            // 统计请求设备的操作系统，使用 request.getHeader("User-Agent")
            String os = LinkUtil.getOs(((HttpServletRequest) request));     // 单独抽出来变量，AccessLogs日志表需要
            LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                    .os(os)
                    .cnt(1)
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkOsStatsMapper.shortLinkOsStats(linkOsStatsDO);

            // 统计请求的浏览器
            String browser = LinkUtil.getBrowser(((HttpServletRequest) request));       // 单独抽出来变量，AccessLogs日志表需要
            LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                    .browser(browser)
                    .cnt(1)
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkBrowserStatsMapper.shortLinkBrowserStats(linkBrowserStatsDO);

            // 统计访问设备
            String device = LinkUtil.getDevice(((HttpServletRequest) request));     // 单独抽出来变量，AccessLogs日志表需要
            LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                    .device(device)
                    .cnt(1)
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkDeviceStatsMapper.shortLinkDeviceStats(linkDeviceStatsDO);

            // 统计访问网络，是wifi还是移动数据
            String network = LinkUtil.getNetwork(((HttpServletRequest) request));   // 单独抽出来变量，AccessLogs日志表需要
            LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                    .network(network)
                    .cnt(1)
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkNetworkStatsMapper.shortLinkNetworkStats(linkNetworkStatsDO);

            // 统计高频访问IP，业务逻辑直接在访问日志表写mysql
            // 将所有关键字段加进日志表里面
            LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                    .user(uv.get())     // 是cookie的标识，不是用户名
                    .ip(remoteAddr)
                    .browser(browser)
                    .os(os)
                    .network(network)
                    .device(device)
                    .locale(StrUtil.join("-", "中国", actualProvince, actualCity))
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .build();
            linkAccessLogsMapper.insert(linkAccessLogsDO);

            // 更新短链接表的历史累计数据字段
            shortLinkMapper.incrementStats(
                    gid,
                    fullShortUrl,
                    1,
                    uvFirstFlag.get() ? 1 : 0,  // incrementUv
                    uipFirstFlag ? 1 : 0);      // incrementUip

            //
            LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())   // 当天日期 年月日
                    .todayPv(1)
                    .todayUv(uvFirstFlag.get() ? 1 : 0)
                    .todayUip(uipFirstFlag ? 1 : 0)
                    .build();
            linkStatsTodayMapper.shortLinkTodayStats(linkStatsTodayDO);


        } catch (Throwable ex) {
            log.error("短链接访问量统计异常", ex);
        }
    }

    /**
     * 生成六位短链接后缀
     *
     * @param requestParam 创建短链接请求参数，需要OriginUrl
     * @return 短链接后缀
     */
    private String generateSuffix(ShortLinkCreateReqDTO requestParam) {
        // 解决生成重复的问题
        int customGenerateCount = 0;
        String shortUri;
        while(true) {
            if (customGenerateCount > 10) {
                throw new ServiceException("短链接频繁生成，请稍后重试");
            }
            String originUrl = requestParam.getOriginUrl();
            originUrl += System.currentTimeMillis();    //哈希函数，相同的输入，永远相同输出，所以每次重试加一个随机变量
            shortUri = HashUtil.hashToBase62(originUrl);
            if(!shortUriCreateCachePenetrationBloomFilter.contains(requestParam.getDomain() + "/" + shortUri)) {
                break;  // 不存在则数据一定不存在
            }
            customGenerateCount++;
        }
        return shortUri;    //查找布隆过滤器，返回存在，可能数据是不存在的
    }

    /**
     * 获取目标网址的图标
     *
     * @param url 目标网址
     * @return 图标
     */
    @SneakyThrows
    private String getFavicon(String url) {
        try {
            // 强制使用 TLS 1.2 / 1.3（可选，针对部分旧服务器握手失败）
            System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");

            // 先判断 URL 是否可访问
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000); // 3秒超时
            connection.setReadTimeout(3000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null; // 请求失败，直接返回
            }

            // 使用 Jsoup 解析 HTML
            Document document = Jsoup.connect(url)
                    .timeout(3000)
                    .ignoreHttpErrors(true)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)") // 模拟浏览器
                    .get();

            Element faviconLink = document.select("link[rel~=(?i)^(shortcut )?icon]").first();
            if (faviconLink != null) {
                return faviconLink.attr("abs:href");
            }

        } catch (SSLHandshakeException e) {
            // SSL 握手失败，不影响业务
            System.err.println("SSLHandshakeException: " + url + "，返回默认图标");
        } catch (IOException e) {
            // 网络异常、超时等
            System.err.println("IOException: " + url + "，返回默认图标");
        } catch (Exception e) {
            // 其他异常兜底
            System.err.println("Unexpected exception: " + url + "，返回默认图标");
        }

        // 返回默认 favicon 或 null
        return "/favicon.ico";
    }
}
