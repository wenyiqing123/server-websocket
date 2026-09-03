package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.CurrentUser;
import cn.wyq.serverwebsocket.annotation.LoginNotRequired;
import cn.wyq.serverwebsocket.common.AjaxResult;
import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.dto.*;
import cn.wyq.serverwebsocket.pojo.entity.UserEmailApplyEntity;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.sentinel.GlobalBlockHandler;
import cn.wyq.serverwebsocket.service.FriendService;
import cn.wyq.serverwebsocket.service.UserService;
import cn.wyq.serverwebsocket.socket.WebSocket;
import cn.wyq.serverwebsocket.utils.BaseContext;
import cn.wyq.serverwebsocket.utils.CacheClient;
import cn.wyq.serverwebsocket.utils.MailUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final FriendService friendService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final MailUtil mailUtil;
    private final UserMapper userMapper;
    private final CacheClient cacheClient;

    @GetMapping("/hello")
    @LoginNotRequired
    @Operation(summary = "点击测试security是否放行", description = "测试security放行接口")
    public String hello() {
        log.info("请求测试 Security 放行白名单接口: /user/hello");
        return "hello";
    }

    @GetMapping("/pending-admins")
    @Operation(summary = "获取待审批的管理员申请列表")
    public Result getPendingAdmins() {
        return Result.success(userService.getPendingAdmins());
    }

    @PutMapping("/approve-admin/{id}")
    @Operation(summary = "通过管理员申请")
    public Result approveAdmin(@PathVariable Integer id) {
        boolean success = userService.approveAdmin(id);
        return success ? Result.success("已通过该用户的管理员申请") : Result.error("操作失败，请稍后重试");
    }

    @PutMapping("/reject-admin/{id}")
    @Operation(summary = "拒绝管理员申请")
    public Result rejectAdmin(@PathVariable Integer id) {
        boolean success = userService.rejectAdmin(id);
        return success ? Result.success("已拒绝该用户的管理员申请") : Result.error("操作失败，请稍后重试");
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录接口", extensions = {
            @Extension(properties = {@ExtensionProperty(name = "x-order", value = "1")})
    })
    public Result login(@RequestBody User user, @RequestParam Integer applyRole) {
        log.info("请求用户登录，尝试登录账号信息: {}", user);
        Result login = userService.login(user, applyRole);
        if (login.get("code").equals(200)) {
            List<String> friendNames = friendService.getMyFriendsNameByUsername(user.getUserName());

            if (friendNames != null && !friendNames.isEmpty()) {
                for (String friendName : friendNames) {
                    WebSocket.sendSystemBroadcast(friendName, "你的好友 [" + user.getUserName() + "] 刚刚上线了！");
                }
            }
        }
        return login;
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody UserEmailDto userEmailDto, @RequestParam Integer applyRole) {
        return Result.success(userService.register(userEmailDto, applyRole));
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

    @GetMapping("/path-email")
    @LoginNotRequired
    @Operation(summary = "通过email获取用户路径", description = "通过email获取用户路径")
    public Result getPathByemail(@RequestParam String email) {
        log.info("请求获取指定用户的头像路径，目标用户名: {}", email);
        String path = userService.getPathByEmail(email);
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
//        UserEmailApplyEntity oneByUsername = userMapper.findOneByUsername(user.getUserName());
        log.info("请求修改用户信息，参数 payload: {}", user);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            log.info("检测到密码修改请求，执行密码重新加密");
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        int result = userService.updateUser(user);
        return AjaxResult.toAjax(result);
    }

    @GetMapping("/info")
    @Operation(summary = "根据用户名获取公开资料")
    public Result getUserInfoByName(@RequestParam String userName) {
        UserEmailApplyEntity user = userMapper.findOneByUsername(userName);
        if (user != null) {
            user.setPassword(null);
            return Result.success(user);
        }
        return Result.error("查无此人");
    }

    /**
     * 分页查询用户
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询用户", description = "分页查询用户接口")
    public PageResult<List<UserEmailApplyEntity>> Userlist(@ParameterObject UserQueryDTO userQueryDTO) {
        log.info("请求分页查询用户列表，查询条件 payload: {}", userQueryDTO);
        return userService.userList(userQueryDTO);
    }

    @GetMapping("/append/list")
    @Operation(summary = "分页查询用户", description = "分页查询用户接口")
    public PageResult<List<UserEntity>> UserAppendlist(@ParameterObject UserQueryDTO userQueryDTO) {
        log.info("请求分页查询用户列表，查询条件 payload: {}", userQueryDTO);
        return userService.userAppendList(userQueryDTO);
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
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("用户列表", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        List<UserExportDTO> userExportDTOList = userService.export(userQueryExportDTO);
        EasyExcel.write(response.getOutputStream(), UserExportDTO.class)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("用户信息")
                .doWrite(userExportDTOList);
    }

    @PostMapping("/import")
    @Operation(summary = "通过Excel批量导入用户")
    public Result importUser(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 使用 EasyExcel 同步读取文件流转换为 DTO 集合
            List<UserImportDTO> userList = EasyExcel.read(file.getInputStream())
                    .head(UserImportDTO.class)
                    .sheet()
                    .doReadSync();

            if (userList == null || userList.isEmpty()) {
                return Result.error("上传的 Excel 为空或没有数据");
            }

            // 2. 调用 Service 层进行业务处理和入库
            userService.importUsers(userList);

            return Result.success("成功导入 " + userList.size() + " 条用户数据");
        } catch (Exception e) {
            log.error("导入用户 Excel 失败", e);
            return Result.error("导入失败，请检查 Excel 模板格式是否正确");
        }
    }
    @GetMapping("/importTemplate")
    @Operation(summary = "下载用户导入模板")
    @LoginNotRequired
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("用户导入模板", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

        // 写入一个空的 ArrayList，这样生成的 Excel 就只有表头，没有数据
        EasyExcel.write(response.getOutputStream(), UserImportDTO.class)
                .sheet("模板")
                .doWrite(new ArrayList<>());
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

    @GetMapping("/current")
    public Result getCurrentUser() {
        int currentId = Math.toIntExact(BaseContext.getCurrentId());
        return Result.success(userService.getCurrentUserById(currentId));
    }

    @PostMapping("/update-email")
    @Operation(summary = "换绑邮箱")
    public Result updateEmail(@RequestBody UpdateEmailDto dto) {
        return userService.updateEmail(dto);
    }

    @PutMapping("/client-update")
    @Operation(summary = "C端用户修改个人资料")
    public Result clientUpdateProfile(@RequestBody User user) {
        if (user == null || user.getUserName() == null || user.getUserName().trim().isEmpty()) {
            throw new ServiceException("用户名不能为空", 400);
        }
        int currentId = Math.toIntExact(BaseContext.getCurrentId());
        UserEmailApplyEntity existingUser = userMapper.getCurrentUserById(currentId);
        if (existingUser == null) {
            return Result.error("用户不存在");
        }

        String newUserName = user.getUserName().trim();
        UserEmailApplyEntity userEmailApplyEntity = userMapper.findOneByUsername(newUserName);
        if (userEmailApplyEntity != null && !Objects.equals(userEmailApplyEntity.getId(), currentId)) {
            throw new ServiceException("该用户名已存在", 400);
        }

        String oldUserName = existingUser.getUserName();
        existingUser.setUserName(newUserName);
        existingUser.setPath(user.getPath());
        userMapper.updateUsernameAvatar(existingUser);
        userService.updateUsernameInGroupAndFriend(oldUserName, newUserName);
        cacheClient.evict("users:id", currentId);
        cacheClient.evict("users:detail", currentId);
        cacheClient.evict("users:path", oldUserName);
        cacheClient.evict("users:path", newUserName);
        cacheClient.evictKey(cacheClient.buildKey("users:all", "all"));
        cacheClient.evictByPrefix("manage:users:apply");
        cacheClient.evictByPrefix("manage:users:append");
        // 发送系统广播：通知本人资料更新成功
        // 💡 这里发送给【新的】用户名，因为如果它当前在线的话，此时 session 可能需要重新校验绑定
        WebSocket.sendSystemBroadcast(newUserName, "个人资料更新成功。");

        return Result.success("更新成功");
    }

    @PutMapping("/client-password")
    @Operation(summary = "C端用户修改密码")
    public Result clientUpdatePassword(@RequestBody Map<String, String> params) {
        String userName = params.get("userName");
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        UserEmailApplyEntity user = userMapper.findOneByUsername(userName);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updatePasswordById(user);
        cacheClient.evict("users:id", user.getId());
        cacheClient.evict("users:detail", user.getId());
        cacheClient.evictKey(cacheClient.buildKey("users:all", "all"));

        // 发送系统广播：通知本人密码修改成功
        WebSocket.sendSystemBroadcast(userName, "登录密码修改成功，下次请使用新密码登录。");

        return Result.success("密码修改成功");
    }

    @PutMapping("/client-email")
    @Operation(summary = "C端用户修改/换绑邮箱")
    public Result clientUpdateEmail(@RequestBody UpdateEmailDto dto) {
        UserEmailApplyEntity user = userMapper.findOneByUsername(dto.getUserName());
        if (user == null) {
            return Result.error("用户状态异常");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            return Result.error("当前密码错误，无法修改邮箱");
        }

        boolean isCodeValid = mailUtil.verifyCode(dto.getNewEmail(), dto.getCode());
        if (!isCodeValid) {
            return Result.error("验证码错误或已过期");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", dto.getNewEmail());
        if (userMapper.selectCount(queryWrapper) > 0) {
            return Result.error("该邮箱已被其他账号绑定，请更换");
        }

        user.setEmail(dto.getNewEmail());
        userMapper.updateEmailById(user);
        cacheClient.evict("users:detail", user.getId());
        cacheClient.evictByPrefix("manage:users:apply");
        cacheClient.evictByPrefix("manage:users:append");

        // 发送系统广播：通知本人邮箱换绑成功
        WebSocket.sendSystemBroadcast(dto.getUserName(), "邮箱换绑成功，当前绑定邮箱为：" + dto.getNewEmail());

        return Result.success("邮箱绑定成功");
    }

    @PostMapping("/login-email")
    @Operation(summary = "邮箱验证码登录")
    @LoginNotRequired
    public Result loginByEmail(@RequestBody EmailLoginDto dto) {
        // 1. 先执行原本的登录逻辑
        Result result = userService.loginByEmail(dto);

        // 🌟 2. 新增广播逻辑：只有登录成功，才向好友广播上线通知
        if (result.get("code").equals(200)) {
            try {
                // 因为 dto 里只有邮箱没有用户名，需要先通过邮箱查出具体的实体对象
                UserEmailApplyEntity user = userMapper.selectUserByEmail(dto.getEmail());

                if (user != null) {
                    String userName = user.getUserName();

                    // 查出该用户的所有好友名称
                    List<String> friendsname = friendService.getMyFriendsNameByUsername(userName);

                    // 遍历好友列表，挨个发送上线广播
                    if (friendsname != null && !friendsname.isEmpty()) {
                        for (String friendName : friendsname) {
                            // 这里可以稍微改改文案，和密码登录做个区分，显得系统更有细节
                            WebSocket.sendSystemBroadcast(friendName, "你的好友 [" + userName + "] 刚刚上线了！");
                        }
                    }
                }
            } catch (Exception e) {
                log.error("发送邮箱登录上线广播失败", e); // 捕获异常，绝不让次要的广播功能阻塞核心的登录逻辑
            }
        }

        return result;
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "忘记密码重置")
    @LoginNotRequired
    public Result forgotPassword(@RequestBody ForgotPasswordDto dto) {
        return userService.forgotPassword(dto);
    }
}
