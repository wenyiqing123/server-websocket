package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.mapper.SysConfigMapper;
import cn.wyq.serverwebsocket.pojo.entity.SysConfig;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.utils.CacheClient;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public Map<String, String> getConfigMap() {
        Map<String, String> configMap = new LinkedHashMap<>();
        for (SysConfig config : list()) {
            if (config != null && config.getConfigKey() != null) {
                configMap.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        return configMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateValues(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            String normalizedKey = key.trim();
            SysConfig existing = lambdaQuery()
                    .eq(SysConfig::getConfigKey, normalizedKey)
                    .one();
            if (existing == null) {
                save(SysConfig.builder()
                        .configKey(normalizedKey)
                        .configValue(entry.getValue() == null ? "" : entry.getValue())
                        .configName(normalizedKey)
                        .build());
            } else {
                existing.setConfigValue(entry.getValue() == null ? "" : entry.getValue());
                super.updateById(existing);
            }
        }
        cacheClient.evict("system:config", "all");
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
