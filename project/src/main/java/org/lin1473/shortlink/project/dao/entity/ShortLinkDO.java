package org.lin1473.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lin1473.shortlink.project.common.database.BaseDO;

import java.util.Date;


/**
 * 短链接实体
 */
@Data
@Builder
@TableName("t_link")
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkDO extends BaseDO {

    /**
     * ID
     */
    private Long id;

    /**
     * 域名
     */
    private String domain;

    /**
     * 短链接
     */
    private String shortUri;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 点击量
     */
    private Integer clickNum;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 启用标识 0：已启用 1：未启用
     */
    private Integer enableStatus;

    /**
     * 创建类型 0：接口创建 1：控制台创建
     */
    private Integer createdType;

    /**
     * 有效期类型 0：永久有效 1：用户自定义
     */
    private Integer validDateType;

    /**
     * 有效期
     */
    private Date validDate;

    /**
     * 描述
     */
    @TableField("`describe`")
    private String describe;

    /**
     * 网站标识
     */
    private String favicon;

    /**
     * 历史累计PV
     */
    private Integer totalPv;

    /**
     * 历史累计UV
     */
    private Integer totalUv;

    /**
     * 历史累计UIP
     */
    private Integer totalUip;

    /**
     * 逻辑字段 exist = false，这三个字段不真实存在于数据库表
     * 我们调用分页查询短链接接口时，需要查询t_link表，同时合并left join t_link_stats_today表，最终返回的结果会多出这三个字段给前端
     */

    /**
     * 今日PV
     */
    @TableField(exist = false)
    private Integer todayPv;

    /**
     * 今日UV
     */
    @TableField(exist = false)
    private Integer todayUv;

    /**
     * 今日UIP
     */
    @TableField(exist = false)
    private Integer todayUip;

}
