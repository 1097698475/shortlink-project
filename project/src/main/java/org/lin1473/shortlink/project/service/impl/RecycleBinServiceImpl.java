package org.lin1473.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.lin1473.shortlink.project.dao.entity.ShortLinkDO;
import org.lin1473.shortlink.project.dao.mapper.ShortLinkMapper;
import org.lin1473.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import org.lin1473.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkRecycleBinPageReqDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.lin1473.shortlink.project.service.RecycleBinService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static org.lin1473.shortlink.project.common.constant.RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY;
import static org.lin1473.shortlink.project.common.constant.RedisKeyConstant.GOTO_SHORT_LINK_KEY;


/**
 * 回收站管理接口实现层
 */
@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements RecycleBinService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void saveRecycleBin(RecycleBinSaveReqDTO requestParam) {
        // where条件
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0);
        // set 字段，链式构造
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .enableStatus(1)    // 1表示不启用
                .build();
        baseMapper.update(shortLinkDO, updateWrapper);

        // 删除相关的缓存，防止短链接跳转
        stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .in(ShortLinkDO::getGid, requestParam.getGidList())     // 不是eq，而是in
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getUpdateTime);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each -> {
            ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);    // 查询到的该分组标识gid下的t_link记录
            result.setDomain("http://" + result.getDomain());   // 单独设置domain字段，将协议类型加进去
            return result;
        });
    }

    @Override
    public void recoverReycleBin(RecycleBinRecoverReqDTO requestParam) {
        // where条件
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus, 1)    // 查询删除的短链接
                .eq(ShortLinkDO::getDelFlag, 0);
        // set 字段，链式构造
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .enableStatus(0)    // 1表示不启用
                .build();
        baseMapper.update(shortLinkDO, updateWrapper);

        // 1. 缓存预热，添加GOTO_SHORT_LINK_KEY，但是这一步可以不做，因为删除的短链接肯定不是热点链接，直接用ShortLinkServiceImpl的跳转，自动添加key到redis即可
        // 2. 删除掉空值跳转前缀，这是因为预防缓存穿透(redis查不到，进而查询数据库，
        // 我们不希望数据库访问压力大），我们对访问不生效（enableStatus=1，或者数据库根本就没有该短链接）的短链接做了空值跳转+跳转锁。
        stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
    }

}
