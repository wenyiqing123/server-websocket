package cn.wyq.serverwebsocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 🌟 核心修复：删除了原来的 exclude，因为对应的包已经被咱们删光了
/**
 * ServerWebsocketApplication：后端领域对象，封装相关数据和处理能力。
 */
@SpringBootApplication
@Slf4j
@EnableCaching //开启redis缓存
@EnableTransactionManagement //开启注解方式的事务管理
@EnableScheduling  //spring task注解：定时任务
@EnableAsync
public class ServerWebsocketApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerWebsocketApplication.class, args);
        log.info("当前时间：" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
        log.info("server started");
    }

    /**
     * 配置 RestTemplate  bean远程调用
     *
     * @return
     */
    @Bean
    public RestTemplate restTemplate() { // 💡 顺手去掉这里的 static，通常 @Bean 方法不需要 static
        return new RestTemplate();
    }
}