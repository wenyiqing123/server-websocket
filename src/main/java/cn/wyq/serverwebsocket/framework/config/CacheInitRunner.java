package cn.wyq.serverwebsocket.framework.config;

import cn.wyq.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wyq.serverwebsocket.pojo.dto.UserQueryDTO;
import cn.wyq.serverwebsocket.service.MessageService;
import cn.wyq.serverwebsocket.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CacheInitRunner {

    @Autowired
    private MessageService messageService;
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 🌟 监听项目启动完成事件
    @EventListener(ApplicationReadyEvent.class)
    public void initCache() {
        System.out.println("🚀 项目启动成功，开始执行缓存预热...");
        // 1. 从数据库查出所有的静态字典数据（比如角色列表）
        MessageQueryDTO messageQueryDTO = MessageQueryDTO.builder().page(1).pageSize(9).build();
        messageService.pageQuery(messageQueryDTO);
        UserQueryDTO userQueryDTO = UserQueryDTO.builder().page(1).pageSize(9).build();
        userService.userList(userQueryDTO);
        System.out.println("✅ 字典数据缓存预热完毕！");
    }
}