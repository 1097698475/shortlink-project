package org.lin1473.shortlink.admin.remote;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.lin1473.shortlink.admin.common.convention.result.Result;
import org.lin1473.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import org.lin1473.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import org.lin1473.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import org.lin1473.shortlink.admin.remote.dto.resp.ShortLinkGroupCountQueryRespDTO;
import org.lin1473.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

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
     * 分页查询短链接
     *
     * @param requestParam 分页短链接请求参数
     * @return 查询短链接响应
     */
    default Result<IPage<ShortLinkPageRespDTO>> pageShortLinks(ShortLinkPageReqDTO requestParam) {
        //Get http方法，param有三个参数，用map拼接
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("gid", requestParam.getGid());
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

}
