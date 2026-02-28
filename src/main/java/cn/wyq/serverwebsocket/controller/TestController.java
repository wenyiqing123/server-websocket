package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.framework.annotation.LoginNotRequired;
import cn.wyq.serverwebsocket.framework.common.AjaxResult;
import cn.wyq.serverwebsocket.framework.common.Result;
import cn.wyq.serverwebsocket.framework.exception.ServiceException;
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
@Tag(name = "测试接口")
public class TestController {
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final TestService testService;
    private final RedisTemplate redisTemplate;

    /**
     * 远程调用"/user/users"
     *
     * @return
     */
    @GetMapping("/remoteProcedureCall")
    @Operation(summary = "远程调用/user/users")
    public Result test() throws IllegalAccessException {
        // 使用 resttemplate 发送 http 请求获取 response
        ResponseEntity<Result> response = restTemplate.exchange(
                "http://localhost:8000/user/users",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Result>() {
                }
        );
        Field[] declaredFields = response.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);
        }
        if (response.getStatusCode().is2xxSuccessful()) {

            // 获取 response 的 body
            Object responseBody = response.getBody();
            // 判断是否为 Result 类型
            if (responseBody instanceof Result) {
                Result result = (Result) responseBody;
                List<User> users = (List<User>) result.get("data");
                //返回属性值
                Class<?> clazz = User.class;
//                User user = new User(1, "张三", "123", null);
                Field userName = null;
                try {
                    userName = clazz.getDeclaredField("userName");
                    userName.setAccessible(true); //如果字段为private，需要设置Accessible
//                    userName.set(user, "value"); //value为要修改为的值
                    Test test = new Test(1, "张三");
                    Class<Test> testClass = Test.class;
                    Method say = testClass.getDeclaredMethod("say", String.class);
                    say.setAccessible(true);
                    Object res = say.invoke(test, "张三");
                } catch (NoSuchFieldException e) {
                    throw new ServiceException("反射获取属性值", 400);
                } catch (IllegalAccessException e) {
                    throw new ServiceException("反射设置属性值", 400);
                } catch (NoSuchMethodException e) {
                    throw new ServiceException("反射获取成员方法", 400);
                } catch (InvocationTargetException e) {
                    throw new ServiceException("反射调用成员方法", 400);
                }
                return Result.success(users);
            }
        }
        return Result.error();
    }

    @GetMapping("/reflect")
    @Operation(summary = "反射测试")
    public void reflect() {
        User user = new User();
        log.info("user.getClass().getSuperclass()", user.getClass().getSuperclass());
        log.info("user.getClass().getInterfaces()", user.getClass().getInterfaces());
        log.info("user.getClass().getDeclaredFields()", user.getClass().getDeclaredFields());
        log.info("user.getClass().getDeclaredMethods()", user.getClass().getDeclaredMethods());
        log.info("user.getClass().getDeclaredConstructors()", user.getClass().getDeclaredConstructors());

    }


    @GetMapping("/stream-message")
    @Operation(summary = "流式返回文本")
    public void streamText(HttpServletResponse response) throws IOException, InterruptedException {
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
    }

    // 测试接口：浏览器访问 http://localhost:8000/test/send?msg=你好
    @GetMapping("/send")
    @LoginNotRequired
    public String sendTask(@RequestParam String msg) {
        // 模拟一个对象数据
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", System.currentTimeMillis());
        data.put("content", msg);
        data.put("type", "AI_DRAWING");

        // 🔥 发送消息：参数1=队列名，参数2=消息对象(会自动转JSON)
        rabbitTemplate.convertAndSend("ai_task_queue", data);

        return msg;
    }

    /**
     * 测试@AutoFill自动填充公共字段和MapStruct使用
     * @param testDTO
     * @return
     */
    @PostMapping("/test-auto-fill")
    @Operation(summary = "测试测试@AutoFill自动填充公共字段和MapStruct使用")
    public AjaxResult<Void> testAutoFill(@RequestBody TestDTO testDTO) {
        testService.testAutoFill(testDTO);
        return AjaxResult.success();
    }
    @GetMapping("/test-redis")
    @LoginNotRequired
    public Result testRedis(){
        redisTemplate.opsForValue().set("name","wyq");
        String name = (String) redisTemplate.opsForValue().get("name");
        return Result.success(name);
    }
}
