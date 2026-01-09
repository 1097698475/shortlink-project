package org.lin1473.shortlink.project.toolkit;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.Optional;

import static org.lin1473.shortlink.project.common.constant.ShortLinkConstant.DEFAULT_CACHE_VALID_TIME;

/**
 * 短链接工具类
 */
public class LinkUtil {

    /**
     * 获取短链接缓存有效期时间
     * 不为null，将有效时间和当前时间做差值，设置
     * 为null,说明短链接永久有效，设置DEFAULT_CACHE_VALID_TIME（一个月）
     *
     * @param validDate 有效期时间
     * @return 有效期时间戳
     */
    public static long getLinkCacheValidTime(Date validDate) {
        return Optional.ofNullable(validDate)
                .map(each -> DateUtil.between(new Date(), each, DateUnit.MS))
                .orElse(DEFAULT_CACHE_VALID_TIME);
    }

    /**
     * 获取用户真实IP
     *
     * @param request 请求
     * @return 用户真实IP
     */
    public static String getActualIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String ip;

        // 1. 标准反向代理 Header（最优先）
        ip = request.getHeader("X-Forwarded-For");
        if (isValid(ip)) {
            // 可能是多个 IP，取第一个
            int commaIndex = ip.indexOf(',');
            return (commaIndex > 0 ? ip.substring(0, commaIndex) : ip).trim();
        }

        // 2. Nginx 常用
        ip = request.getHeader("X-Real-IP");
        if (isValid(ip)) {
            return ip.trim();
        }

        // 3. 兼容历史 / 某些代理
        ip = request.getHeader("Proxy-Client-IP");
        if (isValid(ip)) {
            return ip.trim();
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValid(ip)) {
            return ip.trim();
        }

        // 4. 兜底：直接连接 IP
        return request.getRemoteAddr();
    }

    private static boolean isValid(String ip) {
        return ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip);
    }
}