package cn.wyq.serverwebsocket.config;


import cn.wyq.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wyq.serverwebsocket.pojo.dto.UserQueryDTO;
import cn.wyq.serverwebsocket.service.MessageService;
import cn.wyq.serverwebsocket.service.RealTimeRecommendService;
import cn.wyq.serverwebsocket.service.UserService;
import cn.wyq.serverwebsocket.utils.MailUtil;
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

    @Autowired
    private RealTimeRecommendService recommendService;

    @Autowired
    private MailUtil mailUtil;

    // 🌟 监听项目启动完成事件
    @EventListener(ApplicationReadyEvent.class)
    public void initCache() {
        log.info("项目启动成功，开始执行全局缓存预热");

        // ==========================================
        // 模块 1：基础业务数据（消息、用户列表）预热
        // ==========================================
        try {
            log.info("缓存管理页面用户信息和消息记录信息");
            MessageQueryDTO messageQueryDTO = MessageQueryDTO.builder().page(1).pageSize(9).build();
            messageService.pageQuery(messageQueryDTO);
            UserQueryDTO userQueryDTO = UserQueryDTO.builder().page(1).pageSize(9).build();
            userService.userList(userQueryDTO);
            log.info("缓存成功");
        } catch (Exception e) {
            log.error("❌ 基础业务数据预热失败", e);
        }

        // ==========================================
        // 模块 2：AI 推荐系统协同过滤矩阵冷启动重播
        // ==========================================
        try {
            log.info("缓存ai对话页面矩阵消息信息");
            // 直接调用你刚刚在 RealTimeRecommendService 里写好的重播方法
            recommendService.rebuildCacheIfEmpty();
            log.info("缓存成功");
        } catch (Exception e) {
            log.error("缓存成功", e);
        }

        log.info("所有缓存预热流程执行结束，系统准备就绪");


        /**
         * 发送邮箱验证码
         */
//        String toMail="2829763734@qq.com";
//        mailUtil.sendVerificationCode(toMail);


    }
}