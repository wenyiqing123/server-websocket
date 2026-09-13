package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.CurrentUser;
import cn.wyq.serverwebsocket.annotation.LoginNotRequired;
import cn.wyq.serverwebsocket.common.AjaxResult;
import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.messaging.LoginEventPublisher;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

@RestController
@RequestMapping("/user")
@Validated
@Tag(name = "用户服务", description = "用户注册、登录及管理相关接口", extensions = {
        @Extension(properties = {
                @ExtensionProperty(name = "x-order", value = "1")
        })
})
/**
 * UserController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@Slf4j
@RequiredArgsConstructor
public class UserController {
    private final FriendService friendService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final MailUtil mailUtil;
    private final UserMapper userMapper;
    private final CacheClient cacheClient;
    private final LoginEventPublisher loginEventPublisher;

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
        requireSuperAdministrator();
        return Result.success(userService.getPendingAdmins());
    }

    @PutMapping("/approve-admin/{id}")
    @Operation(summary = "通过管理员申请")
    public Result approveAdmin(@PathVariable Integer id) {
        requireSuperAdministrator();
        boolean success = userService.approveAdmin(id);
        return success ? Result.success("已通过该用户的管理员申请") : Result.error("操作失败，请稍后重试");
    }

    @PutMapping("/reject-admin/{id}")
    @Operation(summary = "拒绝管理员申请")
    public Result rejectAdmin(@PathVariable Integer id) {
        requireSuperAdministrator();
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
    public Result login(@RequestBody User user, @RequestParam Integer applyRole, HttpServletRequest request) {
        log.info("请求用户登录，用户名: {}", user == null ? null : user.getUserName());
        String username = user == null ? null : user.getUserName();
        try {
            Result login = userService.login(user, applyRole);
            User loginUser = username == null ? null : userMapper.findByUsername(username);
            if (isSuccess(login) && loginUser != null) {
                loginEventPublisher.publishLoginSuccess(loginUser.getId(), loginUser.getUserName(),
                        userMapper.findEmailByUsername(loginUser.getUserName()), "PASSWORD_LOGIN_SUCCESS",
                        clientIp(request), request.getHeader("User-Agent"));
                List<String> friendNames = friendService.getMyFriendsNameByUsername(username);
                if (friendNames != null && !friendNames.isEmpty()) {
                    for (String friendName : friendNames) {
                        WebSocket.sendSystemBroadcast(friendName, "你的好友 [" + username + "] 刚刚上线了！");
                    }
                }
            } else {
                loginEventPublisher.publishLoginFailure(loginUser == null ? null : loginUser.getId(), username,
                        "PASSWORD_LOGIN_FAILURE", clientIp(request), request.getHeader("User-Agent"),
                        String.valueOf(login.get("msg")));
            }
            return login;
        } catch (ServiceException e) {
            loginEventPublisher.publishLoginFailure(null, username, "PASSWORD_LOGIN_FAILURE",
                    clientIp(request), request.getHeader("User-Agent"), e.getMessage());
            throw e;
        }
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody UserEmailDto userEmailDto, @RequestParam Integer applyRole,
                           HttpServletRequest request) {
        try {
            int result = userService.register(userEmailDto, applyRole);
            UserEmailApplyEntity registered = userEmailDto == null ? null
                    : userMapper.selectUserByEmail(userEmailDto.getEmail());
            if (registered != null) {
                loginEventPublisher.publishRegistration(registered.getId(), registered.getUserName(),
                        registered.getEmail(), clientIp(request), request.getHeader("User-Agent"));
            }
            return Result.success(result);
        } catch (ServiceException e) {
            loginEventPublisher.publishLoginFailure(null,
                    userEmailDto == null ? null : userEmailDto.getUserName(), "REGISTER_FAILURE",
                    clientIp(request), request.getHeader("User-Agent"), e.getMessage());
            throw e;
        }
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
        requireActiveAdministrator();
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
        UserEmailApplyEntity operator = requireActiveAdministrator();
        assertCanManageTarget(operator, id, null);
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
        UserEmailApplyEntity operator = requireActiveAdministrator();
        if (user == null || user.getId() <= 0) {
            throw new ServiceException("用户编号不能为空", 400);
        }
        assertCanManageTarget(operator, user.getId(), user);
        UserEmailApplyEntity targetBeforeUpdate = userMapper.getCurrentUserById(user.getId());
        String previousUserName = targetBeforeUpdate == null ? null : targetBeforeUpdate.getUserName();
        log.info("请求修改用户信息，目标用户ID: {}", user.getId());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            log.info("检测到密码修改请求，执行密码重新加密");
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        int result = userService.updateUser(user);
        if (result > 0 && previousUserName != null && !previousUserName.equals(user.getUserName())) {
            // 管理员不能代替目标用户签发新凭据。服务事务已经提交后，撤销其刷新凭据并关闭旧身份连接，
            // 下次操作必须以数据库中的新用户名重新登录。
            userService.revokeRefreshToken(user.getId());
            WebSocket.disconnectUser(previousUserName);
        }
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
        requireActiveAdministrator();
        log.info("请求分页查询用户列表，查询条件 payload: {}", userQueryDTO);
        return userService.userList(userQueryDTO);
    }

    @GetMapping("/append/list")
    @Operation(summary = "分页查询用户", description = "分页查询用户接口")
    public PageResult<List<UserEntity>> UserAppendlist(@ParameterObject UserQueryDTO userQueryDTO) {
        requireActiveAdministrator();
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
    public Result getEmail(@RequestBody UserEmailDto userEmailDto, HttpServletRequest request) {
        log.info("请求向邮箱发送验证码，目标邮箱: {}", userEmailDto.getEmail());
        mailUtil.sendVerificationCode(userEmailDto.getEmail());
        UserEmailApplyEntity existing = userMapper.selectUserByEmail(userEmailDto.getEmail());
        loginEventPublisher.publishVerificationRequested(existing == null ? null : existing.getId(),
                userEmailDto.getEmail(), clientIp(request), request.getHeader("User-Agent"));
        return Result.success();
    }

    @GetMapping("/export")
    @Operation(summary = "按条件导出excel", description = "按条件导出excel")
    public void exportUser(@ParameterObject UserQueryExportDTO userQueryExportDTO, HttpServletResponse response) throws IOException {
        requireActiveAdministrator();
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
        requireActiveAdministrator();
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
        User user = userService.findById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @GetMapping()
    @Operation(summary = "获取当前用户信息", description = "获取当前用户信息")
    public Result getUser() {
        int currentId = Math.toIntExact(BaseContext.getCurrentId());
        User user = userService.findById(currentId);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @PatchMapping("/updatePassword")
    public Result updatePassword(@RequestBody UserUpdatePasswordDTO userUpdatePasswordDTO) {
        userService.updatePassword(userUpdatePasswordDTO);
        return Result.success();
    }

    @GetMapping("/current")
    public Result getCurrentUser() {
        int currentId = Math.toIntExact(BaseContext.getCurrentId());
        UserEmailApplyEntity user = userService.getCurrentUserById(currentId);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @PostMapping("/update-email")
    @Operation(summary = "换绑邮箱")
    public Result updateEmail(@RequestBody UpdateEmailDto dto) {
        return userService.updateEmail(dto);
    }

    @PutMapping("/client-update")
    @Operation(summary = "C端用户修改个人资料")
    @Transactional(rollbackFor = Exception.class)
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
        boolean usernameChanged = !oldUserName.equals(newUserName);
        if (!usernameChanged) {
            // 仅头像等资料变动，保持原有响应契约和当前 WebSocket 连接。
            WebSocket.sendSystemBroadcast(newUserName, "个人资料更新成功。");
            return Result.success("更新成功");
        }

        // 事务成功提交后再读取数据库身份、轮换令牌并踢出旧用户名的连接。
        // TransactionInterceptor 会在 HTTP 响应序列化前执行 afterCommit，因此客户端只会收到已提交身份的新令牌。
        Map<String, String> tokenPair = new HashMap<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                tokenPair.putAll(userService.rotateTokensAfterUsernameChange(currentId));
                WebSocket.disconnectUser(oldUserName);
                try {
                    WebSocket.sendSystemBroadcast(newUserName, "个人资料更新成功。");
                } catch (Exception exception) {
                    // 通知失败不能让已提交的新身份和已签发的令牌对无法返回给客户端。
                    log.warn("用户资料已更新，但系统通知发送失败", exception);
                }
            }
        });

        return Result.success("更新成功", tokenPair);
    }

    @PutMapping("/client-password")
    @Operation(summary = "C端用户修改密码")
    public Result clientUpdatePassword(@Valid @RequestBody UserUpdatePasswordDTO params) {
        if (params == null) {
            return Result.error("新旧密码不能为空");
        }
        int currentId = Math.toIntExact(BaseContext.getCurrentId());
        String oldPassword = params.getOldPassword();
        String newPassword = params.getNewPassword();

        // 目标账户只能由已认证的用户 ID 决定，绝不接受请求体中的用户名。
        UserEmailApplyEntity user = userMapper.getCurrentUserById(currentId);
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
        // 前端成功后会主动退出；服务端同时撤销 Refresh Token，阻止旧会话重新换取凭据。
        userService.revokeRefreshToken(user.getId());

        // 通知属于附加能力，不能影响前端按成功结果退出登录。
        try {
            WebSocket.sendSystemBroadcast(user.getUserName(), "登录密码修改成功，下次请使用新密码登录。");
        } catch (Exception exception) {
            log.warn("密码已修改，但系统通知发送失败", exception);
        }

        return Result.success("密码修改成功");
    }

    @PutMapping("/client-email")
    @Operation(summary = "C端用户修改/换绑邮箱")
    public Result clientUpdateEmail(@Valid @RequestBody UpdateEmailDto dto) {
        if (dto == null) {
            return Result.error("邮箱修改参数不能为空");
        }
        int currentId = Math.toIntExact(BaseContext.getCurrentId());
        // 目标账户只能由当前认证身份决定，DTO 中不再存在可伪造的 userName 字段。
        UserEmailApplyEntity user = userMapper.getCurrentUserById(currentId);
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

        // 使用数据库中读取到的用户名通知当前账户，而非请求体字段。
        try {
            WebSocket.sendSystemBroadcast(user.getUserName(), "邮箱换绑成功，当前绑定邮箱为：" + dto.getNewEmail());
        } catch (Exception exception) {
            log.warn("邮箱已换绑，但系统通知发送失败", exception);
        }

        return Result.success("邮箱绑定成功");
    }

    @PostMapping("/login-email")
    @Operation(summary = "邮箱验证码登录")
    @LoginNotRequired
    public Result loginByEmail(@RequestBody EmailLoginDto dto, HttpServletRequest request) {
        // 1. 先执行原本的登录逻辑
        Result result = userService.loginByEmail(dto);
        UserEmailApplyEntity user = dto == null ? null : userMapper.selectUserByEmail(dto.getEmail());

        // 🌟 2. 新增广播逻辑：只有登录成功，才向好友广播上线通知
        if (isSuccess(result) && user != null) {
            loginEventPublisher.publishLoginSuccess(user.getId(), user.getUserName(), user.getEmail(),
                    "EMAIL_LOGIN_SUCCESS", clientIp(request), request.getHeader("User-Agent"));
            try {
                // 因为 dto 里只有邮箱没有用户名，需要先通过邮箱查出具体的实体对象
                String userName = user.getUserName();
                List<String> friendsname = friendService.getMyFriendsNameByUsername(userName);
                if (friendsname != null && !friendsname.isEmpty()) {
                    for (String friendName : friendsname) {
                        WebSocket.sendSystemBroadcast(friendName, "你的好友 [" + userName + "] 刚刚上线了！");
                    }
                }
            } catch (Exception e) {
                log.error("发送邮箱登录上线广播失败", e); // 捕获异常，绝不让次要的广播功能阻塞核心的登录逻辑
            }
        } else {
            loginEventPublisher.publishLoginFailure(user == null ? null : user.getId(),
                    user == null ? (dto == null ? null : dto.getEmail()) : user.getUserName(),
                    "EMAIL_LOGIN_FAILURE", clientIp(request), request.getHeader("User-Agent"),
                    String.valueOf(result.get("msg")));
        }

        return result;
    }

    private boolean isSuccess(Result result) {
        return result != null && Integer.valueOf(200).equals(result.get("code"));
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp.trim();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "忘记密码重置")
    @LoginNotRequired
    public Result forgotPassword(@RequestBody ForgotPasswordDto dto) {
        return userService.forgotPassword(dto);
    }

    /**
     * 从数据库重新读取操作人，避免只相信 JWT 中可能已过期的权限信息。
     */
    private UserEmailApplyEntity requireActiveAdministrator() {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null || currentUserId <= 0) {
            throw new ServiceException("请先登录", 401);
        }
        UserEmailApplyEntity operator = userMapper.getCurrentUserById(Math.toIntExact(currentUserId));
        if (operator == null || !Integer.valueOf(1).equals(operator.getStatus())
                || operator.getRole() == null || (operator.getRole() != 0 && operator.getRole() != 1)) {
            throw new ServiceException("仅状态正常的管理员可以执行此操作", 403);
        }
        return operator;
    }

    /** 管理员审批会改变账号权限，因此只允许超级管理员执行。 */
    private void requireSuperAdministrator() {
        UserEmailApplyEntity operator = requireActiveAdministrator();
        if (operator.getRole() != 0) {
            throw new ServiceException("仅超级管理员可以审批管理员申请", 403);
        }
    }

    /** 普通管理员只能维护普通用户，不能修改管理员账号或调整账号权限。 */
    private void assertCanManageTarget(UserEmailApplyEntity operator, Integer targetUserId, UserEntity update) {
        if (targetUserId == null || targetUserId <= 0) {
            throw new ServiceException("用户编号不能为空", 400);
        }
        UserEmailApplyEntity target = userMapper.getCurrentUserById(targetUserId);
        if (target == null) {
            throw new ServiceException("用户不存在", 404);
        }
        if (operator.getRole() != 0
                && (!Integer.valueOf(2).equals(target.getRole())
                || (update != null && update.getRole() != 2))) {
            throw new ServiceException("管理员不能修改管理员账号或调整角色", 403);
        }
    }
}
