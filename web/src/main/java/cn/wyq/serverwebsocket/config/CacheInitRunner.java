package cn.wyq.serverwebsocket.config;

import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wyq.serverwebsocket.pojo.dto.UserQueryDTO;
import cn.wyq.serverwebsocket.service.MessageService;
import cn.wyq.serverwebsocket.service.RealTimeRecommendService;
import cn.wyq.serverwebsocket.service.UserService;
import cn.wyq.serverwebsocket.utils.MailUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class CacheInitRunner {

    @Autowired
    private MessageService messageService;
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RealTimeRecommendService recommendService;
    @Autowired
    private MailUtil mailUtil;

    // 🌟 1. 注入布隆过滤器需要的核心组件
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private UserMapper userMapper;

    // 🌟 监听项目启动完成事件
    @EventListener(ApplicationReadyEvent.class)
    public void initCache() {
        log.info("项目启动成功，开始执行全局缓存预热");

        // ==========================================
        // 模块 1：基础业务数据（消息、用户列表）预热
        // ==========================================
        try {
            log.info("1. 开始缓存管理页面用户信息和消息记录信息...");
            MessageQueryDTO messageQueryDTO = MessageQueryDTO.builder().page(1).pageSize(9).build();
            messageService.pageQuery(messageQueryDTO);
            UserQueryDTO userQueryDTO = UserQueryDTO.builder().page(1).pageSize(9).build();
            userService.userList(userQueryDTO);
            log.info("基础业务数据缓存成功");
        } catch (Exception e) {
            log.error("基础业务数据预热失败", e);
        }

        // ==========================================
        // 模块 2：AI 推荐系统协同过滤矩阵冷启动重播
        // ==========================================
        try {
            log.info("2. 开始缓存 AI 对话页面矩阵消息信息...");
            recommendService.rebuildCacheIfEmpty();
            log.info("矩阵消息信息缓存成功");
        } catch (Exception e) {
            log.error("矩阵消息信息缓存失败", e); // 💡 帮你修正了这里的报错文案
        }

        // ==========================================
        // 模块 3：布隆过滤器（防穿透）安全名单预热
        // ==========================================
        try {
            log.info("3. 开始初始化并预热【布隆过滤器】...");
            // 获取布隆过滤器对象
            RBloomFilter<Integer> bloomFilter = redissonClient.getBloomFilter("user:bloom:filter");

            // 初始化参数：预计存入 10万 条数据，容忍 1% 的误判率 (0.01)
            bloomFilter.tryInit(100000L, 0.01);

            // 从数据库拉取所有用户 ID
            List<Integer> allUserIds = userMapper.findAllUserIds();

            if (allUserIds != null && !allUserIds.isEmpty()) {
                for (Integer id : allUserIds) {
                    bloomFilter.add(id);
                }
                log.info("布隆过滤器预热完成，共加载了 {} 个安全用户 ID", allUserIds.size());
            } else {
                log.info("布隆过滤器预热完成，当前数据库暂无用户");
            }
        } catch (Exception e) {
            log.error("布隆过滤器预热失败", e);
        }

        log.info("全局缓存预热流程执行结束");
    }
}