package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.mapper.SysConfigMapper;
import cn.wyq.serverwebsocket.pojo.entity.SysConfig;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.utils.CacheClient;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SysConfigServiceImpl：业务服务实现类，封装对应领域的业务规则。
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {
    private final CacheClient cacheClient;

    public SysConfigServiceImpl(CacheClient cacheClient) {
        this.cacheClient = cacheClient;
    }

    @Override
    public List<SysConfig> list() {
        return cacheClient.queryWithMutex("system:config", "all",
                new TypeReference<List<SysConfig>>() {}, ignored -> {
                    List<SysConfig> configs = super.list();
                    return configs == null ? List.of() : configs;
                }, 5, java.util.concurrent.TimeUnit.MINUTES);
    }

    @Override
    public boolean updateById(SysConfig entity) {
        boolean updated = super.updateById(entity);
        if (updated) {
            cacheClient.evict("system:config", "all");
        }
        return updated;
    }
}
