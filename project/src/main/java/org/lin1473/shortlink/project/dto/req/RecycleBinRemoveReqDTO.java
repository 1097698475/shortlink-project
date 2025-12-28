package org.lin1473.shortlink.project.dto.req;


import lombok.Data;

/**
 * 回收站保存 请求参数
 */
@Data
public class RecycleBinRemoveReqDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 完整短链接
     */
    private String fullShortUrl;
}
