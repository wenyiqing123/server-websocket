package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.pojo.entity.SysConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * SysConfigService：业务服务接口，定义对应领域的业务操作契约。
 */
public interface SysConfigService extends IService<SysConfig> {
    /** 获取当前系统配置键值对。 */
    Map<String, String> getConfigMap();

    /** 批量保存配置并刷新配置缓存。 */
    void updateValues(Map<String, String> values);
}
