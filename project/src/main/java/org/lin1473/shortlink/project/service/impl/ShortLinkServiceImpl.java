package org.lin1473.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lin1473.shortlink.project.common.convention.exception.ClientException;
import org.lin1473.shortlink.project.common.convention.exception.ServiceException;
import org.lin1473.shortlink.project.common.enums.VailDateTypeEnum;
import org.lin1473.shortlink.project.dao.entity.ShortLinkDO;
import org.lin1473.shortlink.project.dao.mapper.ShortLinkMapper;
import org.lin1473.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.lin1473.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import org.lin1473.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.lin1473.shortlink.project.service.ShortLinkService;
import org.lin1473.shortlink.project.toolkit.HashUtil;
import org.redisson.api.RBloomFilter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 短链接接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    private final ShortLinkMapper shortLinkMapper;


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
                .build();
        try {
            baseMapper.insert(shortLinkDO);
        } catch (DuplicateKeyException ex) {
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getShortUri, fullShortUrl);
            ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
            if (hasShortLinkDO != null) {
                log.warn("短链接：{} 重复入库", fullShortUrl);
                throw new ServiceException("短链接生成重复");
            }
        }
        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);
        // 链式创建
        ShortLinkCreateRespDTO shortLinkCreateRespDTO = ShortLinkCreateRespDTO.builder()
                .fullShortUrl(shortLinkDO.getFullShortUrl())
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .build();
        return shortLinkCreateRespDTO;
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
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each -> BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
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
}
