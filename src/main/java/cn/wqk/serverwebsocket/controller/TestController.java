package cn.wqk.serverwebsocket.controller;

import cn.wqk.serverwebsocket.framework.common.Result;
import cn.wqk.serverwebsocket.pojo.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@RestController
@RequestMapping("/test")
@CrossOrigin
@Validated
@RequiredArgsConstructor
public class TestController {
    private final RestTemplate restTemplate;

    /**
     * 远程调用"/user/users"
     *
     * @return
     */
    @GetMapping("/remoteProcedureCall")
    public Result test() {
        // 使用 resttemplate 发送 http 请求获取 response
        ResponseEntity<Result> response = restTemplate.exchange(
                "http://localhost:8000/user/users",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Result>() {
                }
        );
        if (response.getStatusCode().is2xxSuccessful()) {
            // 获取 response 的 body
            Object responseBody = response.getBody();
            // 判断是否为 Result 类型
            if (responseBody instanceof Result) {
                Result result = (Result) responseBody;
                List<User> users = (List<User>) result.get("data");
                //返回属性值
                User user = new User();
                Class<?> superclass = user.getClass().getSuperclass();
                System.out.println("superclass = " + superclass);
                Class<?>[] interfaces = user.getClass().getInterfaces();
                System.out.println("interfaces = " + interfaces);
                Field[] declaredFields = user.getClass().getDeclaredFields();
                System.out.println("declaredFields = " + declaredFields);
                Method[] declaredMethods = user.getClass().getDeclaredMethods();
                System.out.println("declaredMethods = " + declaredMethods);
                return Result.success(users);
            }
        }
        return Result.error();
    }

    @GetMapping("/reflect")
    public void reflect() {
        User user = new User();
        System.out.println(user.getClass().getSuperclass());
        System.out.println(user.getClass().getInterfaces());
        System.out.println(user.getClass().getDeclaredFields());
        System.out.println(user.getClass().getDeclaredMethods());
        System.out.println(user.getClass().getDeclaredConstructors());
    }
}
