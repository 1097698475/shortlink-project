package org.lin1473.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.lin1473.shortlink.project.dao.entity.ShortLinkDO;
import org.lin1473.shortlink.project.dto.req.ShortLinkPageReqDTO;

/**
 * 短链接持久层
 */
public interface ShortLinkMapper extends BaseMapper<ShortLinkDO> {

    /**
     * 更新短链接表 访问统计自增
     * 更新total_pv total_uv total_uip字段
     */
    @Update("""
            update 
                t_link 
            set 
                total_pv = total_pv + #{incrementPv}, 
                total_uv = total_uv + #{incrementUv}, 
                total_uip = total_uip + #{incrementUip}
            where 
                gid = #{gid} 
                and full_short_url = #{fullShortUrl}
            """)
    void incrementStats(
            @Param("gid") String gid,
            @Param("fullShortUrl") String fullShortUrl,
            @Param("incrementPv") Integer incrementPv,
            @Param("incrementUv") Integer incrementUv,
            @Param("incrementUip") Integer incrementUip
    );

    /**
     * 分页统计当前用户的短链接
     * 返回排序后的短链接Page
     */
    IPage<ShortLinkDO> selectPageLink(ShortLinkPageReqDTO requestParam);
}
