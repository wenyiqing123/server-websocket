package cn.wqk.serverwebsocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
@ComponentScan("cn.wqk.serverwebsocket.*")
@Slf4j
class ServerWebsocketApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerWebsocketApplication.class, args);
        log.info("当前时间：{}",DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
    }

}
