package cn.wqk.serverwebsocket.controller;

import cn.wqk.serverwebsocket.framework.annotation.CurrentUser;
import cn.wqk.serverwebsocket.framework.common.Result;
import cn.wqk.serverwebsocket.pojo.User;
import cn.wqk.serverwebsocket.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/user")
@CrossOrigin
@Validated
@Api(value = "用户Controller", tags = {"用户相关接口"})
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    //    @LoginNotRequired
    @GetMapping("/hello")
    @ApiOperation(value = "测试security放行接口")
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
    public Result register(@RequestBody User user) {
        //密码加密
        //设置密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        //调service
        int register = userService.register(user);
        return Result.toAjax(register, 401, "该用户已存在");
    }

    @GetMapping("/currentUser")
    public Result testCurrentUser(@CurrentUser User currentUser) {
        return Result.success(currentUser);
    }

    /**
     * 查询所有用户
     *
     * @return
     */
    @GetMapping("/users")
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
    public Result logout() {
        return Result.success("退出登录成功");
    }

    @GetMapping("/path")
    public Result getPath(@RequestParam String username) {
        System.out.println("username = " + username);
        String path = userService.getPath(username);
        return Result.success(path);
    }
}
