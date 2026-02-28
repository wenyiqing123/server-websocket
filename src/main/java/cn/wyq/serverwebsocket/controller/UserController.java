package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.framework.annotation.CurrentUser;
import cn.wyq.serverwebsocket.framework.annotation.LoginNotRequired;
import cn.wyq.serverwebsocket.framework.common.AjaxResult;
import cn.wyq.serverwebsocket.framework.common.PageResult;
import cn.wyq.serverwebsocket.framework.common.Result;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.dto.UserQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/user")
@Validated
@Tag(name = "用户服务", description = "登录", extensions = {
        @Extension(properties = {
                @ExtensionProperty(name = "x-order", value = "1")
        })
})
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    //    @LoginNotRequired
    @GetMapping("/hello")
    @LoginNotRequired
    @Operation(summary = "点击测试security是否放行",
            description = "测试security放行接口")

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
    public Result testCurrentUser(@CurrentUser UserEntity currentUser) {
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
        String path = userService.getPath(username);
        return Result.success(path);
    }


    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据ID物理删除用户")
    public AjaxResult<Void> deleteUser(@PathVariable Integer id) {
        int result = userService.deleteUserById(id); // 请在 service 中实现该方法
        return AjaxResult.toAjax(result);
    }

    /**
     * 修改用户信息
     */
    @PutMapping("/update")
    @Operation(summary = "修改用户", description = "支持修改用户名、权限和头像路径")
    public AjaxResult<Void> updateUser(@RequestBody UserEntity user) {
        // 如果修改了密码，需要重新加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        int result = userService.updateUser(user); // 请在 service 中实现该方法
        return AjaxResult.toAjax(result);
    }

    @GetMapping("/list")
    @Operation(summary = "查询所有用户",
            description = "查询所有用户接口")
    public PageResult<List<UserEntity>> Userlist(@ParameterObject UserQueryDTO userQueryDTO) {
        return userService.userList(userQueryDTO);
    }
    // 刷新 Token 接口
    @PostMapping("/refresh")
    @LoginNotRequired
    public Result refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        Map<String, String> map = userService.refreshToken(refreshToken);
        return Result.success(map);
    }
}
