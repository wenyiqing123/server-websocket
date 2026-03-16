package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.LoginNotRequired;
import cn.wyq.serverwebsocket.common.AjaxResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.pojo.Test;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.dto.TestDTO;
import cn.wyq.serverwebsocket.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
@Validated
@RequiredArgsConstructor
@Slf4j
@Tag(name = "测试接口", description = "用于系统功能验证的测试沙箱")
public class TestController {
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final TestService testService;
    private final RedisTemplate redisTemplate;

    /**
     * 远程调用"/user/users"
     */
    @GetMapping("/remoteProcedureCall")
    @Operation(summary = "远程调用 /user/users")
    public Result test() {
        log.info("执行测试: 远程调用(RestTemplate) 获取用户列表");
        try {
            // 使用 resttemplate 发送 http 请求获取 response
            ResponseEntity<Result> response = restTemplate.exchange(
                    "http://localhost:8000/user/users",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result>() {}
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // 获取 response 的 body
                Object responseBody = response.getBody();

                // 判断是否为 Result 类型
                if (responseBody instanceof Result) {
                    Result result = (Result) responseBody;
                    List<User> users = (List<User>) result.get("data"); // Assuming getData() exists

                    // 反射测试逻辑
                    Class<?> clazz = User.class;
                    Field userName = clazz.getDeclaredField("userName");
                    userName.setAccessible(true);

                    Test test = new Test(1, "张三");
                    Class<Test> testClass = Test.class;
                    Method say = testClass.getDeclaredMethod("say", String.class);
                    say.setAccessible(true);
                    Object res = say.invoke(test, "张三");

                    log.info("远程调用测试成功，返回用户数量: {}", users != null ? users.size() : 0);
                    return Result.success(users);
                }
            }
            log.warn("远程调用失败，状态码: {}", response.getStatusCode());
            return Result.error("远程调用失败");

        } catch (NoSuchFieldException e) {
            log.error("反射测试失败: 获取属性值异常", e);
            throw new ServiceException("反射获取属性值失败", 400);
        } catch (NoSuchMethodException e) {
            log.error("反射测试失败: 获取成员方法异常", e);
            throw new ServiceException("反射获取成员方法失败", 400);
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error("反射测试失败: 调用成员方法异常", e);
            throw new ServiceException("反射调用成员方法失败", 400);
        } catch (Exception e) {
            log.error("远程调用测试发生未知异常", e);
            throw new ServiceException("远程调用异常: " + e.getMessage(), 500);
        }
    }

    @GetMapping("/reflect")
    @Operation(summary = "反射测试")
    public void reflect() {
        log.info("执行测试: 获取 User 类的反射信息");
        User user = new User();
        // Fixed logging format to correctly substitute variables
        log.info("Superclass: {}", user.getClass().getSuperclass());
        log.info("Interfaces: {}", (Object) user.getClass().getInterfaces());
        log.info("DeclaredFields: {}", (Object) user.getClass().getDeclaredFields());
        log.info("DeclaredMethods: {}", (Object) user.getClass().getDeclaredMethods());
        log.info("DeclaredConstructors: {}", (Object) user.getClass().getDeclaredConstructors());
    }

    @GetMapping("/stream-message")
    @Operation(summary = "流式返回文本 (SSE)")
    public void streamText(HttpServletResponse response) throws IOException, InterruptedException {
        log.info("执行测试: SSE 流式文本返回模拟");
        response.setContentType("text/event-stream"); // SSE
        response.setCharacterEncoding("UTF-8");

        String text = "这是一个逐字返回的示例文本。";
        ServletOutputStream out = response.getOutputStream();

        for (char c : text.toCharArray()) {
            String data = "data: " + c + "\n\n"; // SSE 格式
            out.write(data.getBytes(StandardCharsets.UTF_8));
            out.flush(); // 立即发送
            Thread.sleep(200); // 每 200ms 发一个字，模拟打字效果
        }
        out.close();
        log.info("SSE 流式文本返回结束");
    }

    // 测试接口：浏览器访问 http://localhost:8000/test/send?msg=你好
    @GetMapping("/send")
    @LoginNotRequired
    @Operation(summary = "发送 RabbitMQ 消息测试")
    public String sendTask(@RequestParam String msg) {
        log.info("执行测试: 发送 RabbitMQ 消息, 消息内容: {}", msg);

        // 模拟一个对象数据
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", System.currentTimeMillis());
        data.put("content", msg);
        data.put("type", "AI_DRAWING");

        // 发送消息：参数1=队列名，参数2=消息对象(会自动转JSON)
        rabbitTemplate.convertAndSend("ai_task_queue", data);

        log.info("RabbitMQ 消息发送成功到队列 'ai_task_queue'");
        return "消息投递成功: " + msg;
    }

    /**
     * 测试@AutoFill自动填充公共字段和MapStruct使用
     */
    @PostMapping("/test-auto-fill")
    @Operation(summary = "测试 @AutoFill 自动填充与 MapStruct")
    public AjaxResult<Void> testAutoFill(@RequestBody TestDTO testDTO) {
        log.info("执行测试: @AutoFill 与 MapStruct, 参数 payload: {}", testDTO);
        testService.testAutoFill(testDTO);
        return AjaxResult.success();
    }

    @GetMapping("/test-redis")
    @LoginNotRequired
    @Operation(summary = "测试 Redis 读写")
    public Result testRedis(){
        log.info("执行测试: Redis 键值读写");
        redisTemplate.opsForValue().set("name", "wyq");
        String name = (String) redisTemplate.opsForValue().get("name");
        log.info("Redis 读取结果: name = {}", name);
        return Result.success(name);
    }
}