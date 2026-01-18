package org.lin1473.shortlink.admin.remote;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.lin1473.shortlink.admin.common.convention.result.Result;
import org.lin1473.shortlink.admin.remote.dto.req.*;
import org.lin1473.shortlink.admin.remote.dto.resp.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 短链接中台远程调用服务
 */
public interface ShortLinkRemoteService {

    /**
     * 创建短链接
     *
     * @param requestParam 创建短链接请求参数
     * @return 短链接创建响应
     */
    default Result<ShortLinkCreateRespDTO> createShortLink(ShortLinkCreateReqDTO requestParam) {
        // 使用post方法调用短链接中心的接口（8001是中心，8002是后管），接口的响应DTO返回为String格式的json
        String resultBodyStr = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/create", JSON.toJSONString(requestParam));
        return JSON.parseObject(resultBodyStr, new TypeReference<>() {
        });
    }

    /**
     * 修改短链接
     *
     * @param requestParam 修改短链接请求参数
     */
    default void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        // 使用post方法调用短链接中心的接口（8001是中心，8002是后管），接口的响应DTO返回为String格式的json
        HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/update", JSON.toJSONString(requestParam));
    }

    /**
     * 分页查询该用户的短链接
     *
     * @param requestParam 分页短链接请求参数
     * @return 查询短链接响应
     */
    default Result<IPage<ShortLinkPageRespDTO>> pageShortLinks(ShortLinkPageReqDTO requestParam) {
        //Get http方法，param有三个参数，用map拼接
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("gid", requestParam.getGid());
        requestMap.put("orderTag", requestParam.getOrderTag());
        requestMap.put("current", requestParam.getCurrent());
        requestMap.put("size", requestParam.getSize());
        // 使用get方法调用短链接中心的接口（8001是中心，8002是后管），接口的响应DTO返回为String格式的json
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/page", requestMap);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {
        });
    }

    /**
     * 查询分组内短链接总量
     *
     * @param requestParam 分组内短链接总量请求参数
     * @return  分组内短链接总量响应
     */
    default Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(List<String> requestParam) {
        //Get http方法，param有三个参数，用map拼接
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("requestParam", requestParam);
        // 使用get方法调用短链接中心的接口（8001是中心，8002是后管），接口的响应DTO返回为String格式的json
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/count", requestMap);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {
        });
    }

    /**
     * 根据 URL 获取标题
     *
     * @param url 目标网站地址
     * @return 网站标题
     */
    default Result<String> getTitleByUrl(@RequestParam("url") String url) {
        String resultStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/title?url=" + url);
        return JSON.parseObject(resultStr, new TypeReference<>() {
        });
    }

    /**
     * 保存回收站
     *
     * @param requestParam 请求参数
     */
    default void saveRecycleBin(RecycleBinSaveReqDTO requestParam) {
        // 使用post方法调用短链接中心的接口（8001是中心，8002是后管），接口的响应DTO返回为String格式的json
        HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/save", JSON.toJSONString(requestParam));
    }

    /**
     * 分页查询回收站短链接
     *
     * @param requestParam 分页短链接请求参数
     * @return 查询短链接响应
     */
    default Result<IPage<ShortLinkPageRespDTO>> pageRecycleBinShortLinks(ShortLinkRecycleBinPageReqDTO requestParam) {
        //Get http方法，param有三个参数，用map拼接
        Map<String, Object> requestMap = new HashMap<>();
        // 不是传入一个gid，而是传入当前用户的所有gid（这里不同于短链接分页查询）
//        requestMap.put("gid", requestParam.getGid());
        requestMap.put("gidList", requestParam.getGidList());
        requestMap.put("current", requestParam.getCurrent());
        requestMap.put("size", requestParam.getSize());
        // 使用get方法调用短链接中心的接口（8001是中心，8002是后管），接口的响应DTO返回为String格式的json
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/page", requestMap);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {
        });
    }

    /**
     * 从回收站恢复短链接
     *
     * @param requestParam 请求参数
     */
    default void recoverReycleBin(RecycleBinRecoverReqDTO requestParam) {
        // 使用post方法调用短链接中心的接口（8001是中心，8002是后管），接口的响应DTO返回为String格式的json
        HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/recover", JSON.toJSONString(requestParam));
    }

    /**
     * 从回收站移除短链接
     *
     * @param requestParam 短链接移除请求参数
     */
    default void removeRecycleBin(RecycleBinRemoveReqDTO requestParam) {
        HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/remove", JSON.toJSONString(requestParam));
    }

    /**
     * 访问单个短链接指定时间内监控数据
     *
     * @param requestParam 访问短链接监控请求参数
     * @return 短链接监控信息
     */
    default Result<ShortLinkStatsRespDTO> oneShortLinkStats(ShortLinkStatsReqDTO requestParam) {
        String resultBodyStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/stats", BeanUtil.beanToMap(requestParam));
        return JSON.parseObject(resultBodyStr, new TypeReference<>() {
        });
    }

    /**
     * 获取单个短链接指定时间内的访问记录数据
     *
     * @param requestParam 获取短链接监控访问记录数据入参
     * @return 访问记录监控数据
     */
    default Result<IPage<ShortLinkStatsAccessRecordRespDTO>> oneShortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(requestParam, false, true);
        stringObjectMap.remove("orders");
        stringObjectMap.remove("records");
        String resultBodyStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/stats/access-record", stringObjectMap);
        return JSON.parseObject(resultBodyStr, new TypeReference<>() {
        });
    }
}
