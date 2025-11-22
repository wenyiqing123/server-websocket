package cn.wqk.serverwebsocket.controller;

import cn.wqk.serverwebsocket.framework.common.Result;
import cn.wqk.serverwebsocket.framework.exception.ServiceException;
import cn.wqk.serverwebsocket.pojo.Test;
import cn.wqk.serverwebsocket.pojo.User;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/test")
@CrossOrigin
@Validated
@RequiredArgsConstructor
@Slf4j
public class TestController {
    private final RestTemplate restTemplate;

    /**
     * 远程调用"/user/users"
     *
     * @return
     */
    @GetMapping("/remoteProcedureCall")
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
        System.out.println("response = " + response);
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);
            System.out.println(declaredField.getName());
            System.out.println(declaredField.get(response));
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
    public void reflect() {
        User user = new User();
        log.info("user.getClass().getSuperclass()", user.getClass().getSuperclass());
        log.info("user.getClass().getInterfaces()", user.getClass().getInterfaces());
        log.info("user.getClass().getDeclaredFields()", user.getClass().getDeclaredFields());
        log.info("user.getClass().getDeclaredMethods()", user.getClass().getDeclaredMethods());
        log.info("user.getClass().getDeclaredConstructors()", user.getClass().getDeclaredConstructors());

    }


    @GetMapping("/stream-message")
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
}
