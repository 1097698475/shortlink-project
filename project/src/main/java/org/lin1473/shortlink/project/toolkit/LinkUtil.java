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

    /**
     * 获取用户访问操作系统
     *
     * @param request 请求
     * @return 访问操作系统
     */
    public static String getOs(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.toLowerCase().contains("windows")) {
            return "Windows";
        } else if (userAgent.toLowerCase().contains("mac")) {
            return "Mac OS";
        } else if (userAgent.toLowerCase().contains("linux")) {
            return "Linux";
        } else if (userAgent.toLowerCase().contains("android")) {
            return "Android";
        } else if (userAgent.toLowerCase().contains("iphone") || userAgent.toLowerCase().contains("ipad")) {
            return "iOS";
        } else {
            return "Unknown";
        }
    }

    /**
     * 获取用户访问浏览器
     * 浏览器厂商为了兼容老网页，会在自己的 User-Agent 中伪装成其他浏览器，比如chrome浏览器也有Safari字段，告诉网站它支持 Safari 的特性。
     *
     * @param request 请求
     * @return 访问浏览器
     */
    public static String getBrowser(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase(); // 转小写

        if (userAgent.contains("edg")) { // Edge
            return "Microsoft Edge";
        } else if (userAgent.contains("opr") || userAgent.contains("opera")) { // Opera
            return "Opera";
        } else if (userAgent.contains("chrome")) { // Chrome (排除 Edge 和 Opera)
            return "Google Chrome";
        } else if (userAgent.contains("firefox")) { // Firefox
            return "Mozilla Firefox";
        } else if (userAgent.contains("safari")) { // Safari (排除 Chrome)
            return "Apple Safari";
        } else {
            return "Other";
        }
    }

    /**
     * 获取用户访问设备
     *
     * @param request 请求
     * @return 访问设备
     */
    public static String getDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.toLowerCase().contains("mobile")) {
            return "Mobile";
        }
        return "PC";
    }

    private static boolean isValid(String ip) {
        return ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip);
    }
}