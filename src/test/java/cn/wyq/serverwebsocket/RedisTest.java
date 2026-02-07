package cn.wyq.serverwebsocket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

//SpringBootTest启动时不会启动服务器，所以WebSocket会报错
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RedisTest {
    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testRedis() {
        System.out.println(redisTemplate);

    }
}
