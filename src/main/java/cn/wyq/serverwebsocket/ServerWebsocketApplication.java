package cn.wyq.serverwebsocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
//如果启动类和mapper文件夹位于同一层级中，则不需要添加该注解
//@ComponentScan("cn.wyq.serverwebsocket.*")
@Slf4j
@EnableCaching //开启redis缓存
@EnableTransactionManagement //开启注解方式的事务管理
class ServerWebsocketApplication {
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
    public static RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
