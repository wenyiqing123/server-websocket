package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.CurrentUser;
import cn.wyq.serverwebsocket.annotation.LoginNotRequired;
import cn.wyq.serverwebsocket.common.AjaxResult;
import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.dto.*;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.sentinel.GlobalBlockHandler;
import cn.wyq.serverwebsocket.service.UserService;
import cn.wyq.serverwebsocket.utils.BaseContext;
import cn.wyq.serverwebsocket.utils.MailUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Validated
@Tag(name = "用户服务", description = "用户注册、登录及管理相关接口", extensions = {
        @Extension(properties = {
                @ExtensionProperty(name = "x-order", value = "1")
        })
})
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final MailUtil mailUtil;

    @GetMapping("/hello")
    @LoginNotRequired
    @Operation(summary = "点击测试security是否放行", description = "测试security放行接口")
    public String hello() {
        log.info("请求测试 Security 放行白名单接口: /user/hello");
        return "hello";
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录接口", extensions = {
            @Extension(properties = {@ExtensionProperty(name = "x-order", value = "1")})
    })
    public Result login(@RequestBody User user) {
        log.info("请求用户登录，尝试登录账号信息: {}", user);
        return userService.login(user);
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册接口")
    public Result register(@RequestBody UserEmailDto userEmailDto) {
        log.info("请求用户注册，注册信息 payload: {}", userEmailDto);
        // 密码加密设置
        userEmailDto.setPassword(passwordEncoder.encode(userEmailDto.getPassword()));
        // 调 service
        int register = userService.register(userEmailDto);
        return Result.toAjax(register, 401, "该用户已存在");
    }

    /**
     * 获取当前用户
     */
    @GetMapping("/currentUser")
    @Operation(summary = "获取当前用户", description = "获取当前登录用户信息接口")
    public Result testCurrentUser(@CurrentUser UserEntity currentUser) {
        log.info("请求获取当前登录用户信息，当前解析用户为: {}", currentUser != null ? currentUser.getUserName() : "未解析到用户");
        return Result.success(currentUser);
    }

    /**
     * 查询所有用户
     */
    @GetMapping("/users")
    @Operation(summary = "查询所有用户", description = "查询所有用户接口")
    public Result findAllUsers() {
        log.info("请求查询全量用户列表 (非分页)");
        List<User> allUsers = userService.findAllUsers();
        return Result.success(allUsers);
    }

    /**
     * 登出
     */
    @GetMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出接口")
    public Result logout() {
        log.info("请求执行用户登出操作");
        return Result.success("退出登录成功");
    }

    /**
     * 获取用户路径(头像等)
     */
    @GetMapping("/path")
    @Operation(summary = "获取用户路径", description = "根据用户名获取用户路径接口")
    public Result getPath(@RequestParam String username) {
        log.info("请求获取指定用户的头像路径，目标用户名: {}", username);
        String path = userService.getPath(username);
        return Result.success(path);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据ID物理删除用户")
    public AjaxResult<Void> deleteUser(@PathVariable Integer id) {
        log.info("请求物理删除用户，待删除用户ID: {}", id);
        int result = userService.deleteUserById(id);
        return AjaxResult.toAjax(result);
    }


    /**
     * 修改用户信息
     */
    @PutMapping("/update")
    @Operation(summary = "修改用户", description = "支持修改用户名、权限和头像路径")
    public AjaxResult<Void> updateUser(@RequestBody UserEntity user) {
        log.info("请求修改用户信息，参数 payload: {}", user);
        // 如果修改了密码，需要重新加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            log.info("检测到密码修改请求，执行密码重新加密");
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        int result = userService.updateUser(user);
        return AjaxResult.toAjax(result);
    }

    /**
     * 分页查询用户
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询用户", description = "分页查询用户接口")
    public PageResult<List<UserEntity>> Userlist(@ParameterObject UserQueryDTO userQueryDTO) {
        log.info("请求分页查询用户列表，查询条件 payload: {}", userQueryDTO);
        return userService.userList(userQueryDTO);
    }

    /**
     * 刷新 Token 接口
     */
    @PostMapping("/refresh")
    @LoginNotRequired
    @Operation(summary = "刷新Token", description = "使用 Refresh Token 获取新的 Access Token")
    public Result refresh(@RequestBody Map<String, String> body) {
        log.info("请求刷新 Access Token");
        String refreshToken = body.get("refreshToken");
        Map<String, String> map = userService.refreshToken(refreshToken);
        return Result.success(map);
    }

    /**
     * 发送验证码
     */
    @PostMapping("/email")
    @LoginNotRequired
    @Operation(summary = "向指定邮箱发送验证码", description = "向指定邮箱发送注册/找回验证码")
    public Result getEmail(@RequestBody UserEmailDto userEmailDto) {
        log.info("请求向邮箱发送验证码，目标邮箱: {}", userEmailDto.getEmail());
        mailUtil.sendVerificationCode(userEmailDto.getEmail());
        return Result.success();
    }

    @GetMapping("/export")
    @Operation(summary = "按条件导出excel", description = "按条件导出excel")
    public void exportUser(@ParameterObject UserQueryExportDTO userQueryExportDTO, HttpServletResponse response) throws IOException {
        log.info("导出用户列表到excel：{}", userQueryExportDTO);
        // 1. 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("用户列表", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        // 2. 获取数据（模拟从 service 获取）
        List<UserExportDTO> userExportDTOList = userService.export(userQueryExportDTO);
        // 3. 一行代码写出
        EasyExcel.write(response.getOutputStream(), UserExportDTO.class)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("用户信息")
                .doWrite(userExportDTOList);
    }

    @SentinelResource(value = "getUser",
            blockHandler = "handleGetUserBlock",
            blockHandlerClass = GlobalBlockHandler.class)
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询用户", description = "根据id查询用户")
    public Result getUser(@PathVariable int id) {
        return Result.success(userService.findById(id));
    }

    @GetMapping()
    @Operation(summary = "获取当前用户信息", description = "获取当前用户信息")
    public Result getUser() {
        int currentId = Math.toIntExact(BaseContext.getCurrentId());
        return Result.success(userService.findById(currentId));
    }

    @PatchMapping("/updatePassword")
    public Result updatePassword(@RequestBody UserUpdatePasswordDTO userUpdatePasswordDTO) {
        userService.updatePassword(userUpdatePasswordDTO);
        return Result.success();
    }

}