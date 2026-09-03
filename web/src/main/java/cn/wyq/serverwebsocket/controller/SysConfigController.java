package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.entity.SysConfig;
import cn.wyq.serverwebsocket.service.SysConfigService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SysConfigController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@RestController
@RequestMapping("/sys-config")
@Tag(name = "系统设置接口")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 获取所有系统配置
     * 前端收到的是类似于 { "theme_color": "#409EFF", "ai_top_k": "3" } 这样的纯键值对对象
     */
    @GetMapping("/getAll")
    public Result getAllConfigs() {
        // 1. 从数据库查出所有配置 List<SysConfig>
        List<SysConfig> configList = sysConfigService.list();

        // 2. 利用 Java 8 Stream 优雅地将其转换为 Map<String, String>
        Map<String, String> configMap = configList.stream()
                .collect(Collectors.toMap(
                        SysConfig::getConfigKey,
                        SysConfig::getConfigValue
                ));

        return Result.success(configMap);
    }

    /**
     * 批量保存系统配置
     * 前端传过来一个 JSON 对象 (Map)
     */
    @PostMapping("/updateBatch")
    public Result updateBatch(@RequestBody Map<String, String> configMap) {

        // 遍历前端传来的 Map，根据 Key 更新对应的 Value
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // MyBatis-Plus 的 UpdateWrapper 用法示例
            sysConfigService.update(
                    new UpdateWrapper<SysConfig>()
                            .eq("config_key", key)
                            .set("config_value", value)
            );
        }

        // 🚀 核心：如果是修改了 AI 提示词等配置，建议在这里去刷新你的 Redis 缓存
        // 或者直接通知大模型 Service 重新加载配置

        return Result.success("保存成功");
    }
}