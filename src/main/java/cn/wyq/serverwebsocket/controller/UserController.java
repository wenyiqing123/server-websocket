package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.framework.annotation.CurrentUser;
import cn.wyq.serverwebsocket.framework.common.Result;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.service.UserService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/user")
@CrossOrigin
@Validated
@Tag(name = "用户服务", description = "登录", extensions = {
        @Extension(properties = {
                @ExtensionProperty(name = "x-order", value = "1")
        })
})

public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    //    @LoginNotRequired
    @GetMapping("/hello")
    @Operation(summary = "点击测试security是否放行",
            description = "测试security放行接口")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "hello")
    })
    public String hello() {
        return "hello";
    }

    /**
     * 登录
     *
     * @param user
     * @return
     */
//    @LoginNotRequired
    @ApiResponses(value = {
            @ApiResponse(code = 200, response = User.class, message = "")
    })
    @Operation(
            summary = "用户登录",
            description = "用户登录接口",
            extensions = {
            @Extension(
                    properties = {
                    @ExtensionProperty(name = "x-order", value = "1")
            })
    })
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        System.out.println("user = " + user);
        return userService.login(user);
    }

    /**
     * 注册
     *
     * @param user
     * @return
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册",
            description = "用户注册接口")
    public Result register(@RequestBody User user) {
        //密码加密
        //设置密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        //调service
        int register = userService.register(user);
        return Result.toAjax(register, 401, "该用户已存在");
    }

    @GetMapping("/currentUser")
    @Operation(summary = "获取当前用户",
            description = "获取当前登录用户信息接口")
    public Result testCurrentUser(@CurrentUser User currentUser) {
        return Result.success(currentUser);
    }

    /**
     * 查询所有用户
     *
     * @return
     */
    @GetMapping("/users")
    @Operation(summary = "查询所有用户",
            description = "查询所有用户接口")
    public Result findAllUsers() {
        List<User> allUsers = userService.findAllUsers();
        return Result.success(allUsers);
    }

    /**
     * 登出
     *
     * @return
     */
    @GetMapping("/logout")
    @Operation(summary = "用户登出",
            description = "用户登出接口")
    public Result logout() {
        return Result.success("退出登录成功");
    }

    @GetMapping("/path")
    @Operation(summary = "获取用户路径",
            description = "根据用户名获取用户路径接口")
    public Result getPath(@RequestParam String username) {
        System.out.println("username = " + username);
        String path = userService.getPath(username);
        return Result.success(path);
    }
}
