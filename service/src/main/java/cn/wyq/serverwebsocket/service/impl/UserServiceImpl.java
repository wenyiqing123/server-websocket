package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.constant.BloomConstant;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.ChatGroupMapper;
import cn.wyq.serverwebsocket.mapper.FriendMapper;
import cn.wyq.serverwebsocket.mapper.MessageMapper;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.dto.*;
import cn.wyq.serverwebsocket.pojo.entity.UserEmailApplyEntity;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.pojo.vo.UserVo;
import cn.wyq.serverwebsocket.service.UserService;
import cn.wyq.serverwebsocket.utils.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final RedisUtil redisUtil;

    private final MailUtil mailUtil;



    private final CacheClient cacheClient;
    private final ChatGroupMapper chatGroupMapper;
    private final MessageMapper messageMapper;
    private final FriendMapper friendMapper;
    private final RedissonClient redissonClient;


    @Override
    public PageResult<List<UserEmailApplyEntity>> userList(UserQueryDTO userQueryDTO) {
        UserQueryDTO query = userQueryDTO == null ? new UserQueryDTO() : userQueryDTO;
        return cacheClient.queryWithMutex(
                "manage:users:apply", userQueryKey(query),
                new TypeReference<PageResult<List<UserEmailApplyEntity>>>() {},
                ignored -> {
                    PageHelper.startPage(query.getPage(), query.getPageSize());
                    Page<UserEmailApplyEntity> page = userMapper.selectAllUsers(query);
                    return PageResult.success(page.getTotal(), page.getResult());
                }, 10, TimeUnit.MINUTES);
    }

    @Override
    public PageResult<List<UserEntity>> userAppendList(UserQueryDTO userQueryDTO) {
        UserQueryDTO query = userQueryDTO == null ? new UserQueryDTO() : userQueryDTO;
        return cacheClient.queryWithMutex(
                "manage:users:append", userQueryKey(query),
                new TypeReference<PageResult<List<UserEntity>>>() {},
                ignored -> {
                    PageHelper.startPage(query.getPage(), query.getPageSize());
                    Page<UserEntity> page = userMapper.selectAllAppendUsers(query);
                    return PageResult.success(page.getTotal(), page.getResult());
                }, 10, TimeUnit.MINUTES);
    }

    @Override
    public Result searchUser(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error("搜索关键词不能为空");
        }

        String normalizedKeyword = keyword.trim();
        return cacheClient.queryWithMutex(
                "users:search", normalizedKeyword,
                Result.class,
                ignored -> {
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    queryWrapper.like("user_name", normalizedKeyword);
                    queryWrapper.select("id", "user_name", "path");
                    queryWrapper.last("LIMIT 20");
                    List<User> userList = userMapper.selectList(queryWrapper);
                    return userList.isEmpty() ? Result.success("未搜索到相关用户") : Result.success(userList);
                }, 5, TimeUnit.MINUTES);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 🌟 极其重要：保证级联删除的强一致性
    public int deleteUserById(Integer id) {
        UserEmailApplyEntity userEntity = userMapper.getCurrentUserById(id);
        if (userEntity == null) {
            return 0; // 用户不存在直接返回
        }
        String userName = userEntity.getUserName();

        // ================= 1. 处理好友关系 =================
        // 清理双向好友关系 (SQL: DELETE FROM friend WHERE user_name = #{userName} OR friend_name = #{userName})
        friendMapper.deleteAllRelationsByUserName(userName);

        // ================= 2. 处理作为群主的群聊 (直接解散) =================
        List<String> ownedGroupIds = chatGroupMapper.findGroupIdsByOwner(userName);
        for (String groupId : ownedGroupIds) {
            // 2.1 删除群主表记录
            chatGroupMapper.deleteGroupById(groupId);
            // 2.2 删除该群所有成员
            chatGroupMapper.deleteAllMembersByGroupId(groupId);
            // 2.3 🌟 连带删除该群的聊天记录 (SQL: DELETE FROM message WHERE to_name = 'GROUP_' + groupId)
            messageMapper.deleteMessagesByTarget("GROUP_" + groupId);
        }

        // ================= 3. 处理作为普通成员的群聊 (退群 + 检查人数) =================
        List<String> joinedGroupIds = chatGroupMapper.findGroupIdsByMember(userName);
        for (String groupId : joinedGroupIds) {
            // 3.1 退出群聊
            chatGroupMapper.deleteGroupMember(groupId, userName);

            // 3.2 检查剩余人数
            int remainCount = chatGroupMapper.countGroupMembers(groupId);
            if (remainCount < 3) {
                // 🌟 触发自动解散机制
                chatGroupMapper.deleteGroupById(groupId);
                chatGroupMapper.deleteAllMembersByGroupId(groupId);
                messageMapper.deleteMessagesByTarget("GROUP_" + groupId);
            }
        }

        // ================= 4. 处理个人聊天记录 =================
        // 删除该用户发送的所有消息，以及作为接收方的所有私聊消息
        // (SQL: DELETE FROM message WHERE from_name = #{userName} OR to_name = #{userName})
        messageMapper.deleteAllMessagesByUserName(userName);

        // ================= 5. 删除用户本身 =================
        int result = userMapper.deleteById(id);

        // ================= 6. 清理各种 Redis 缓存 =================
        // 清理后台分页列表缓存
        evictUserCaches(id);
        // 🌟 强制将该用户踢下线
        redisUtil.delete("refreshToken:userId:" + id);

        return result;
    }

    @Override
    public int updateUser(UserEntity user) {
        if (user == null || user.getId() <= 0 || user.getUserName() == null || user.getUserName().trim().isEmpty()) {
            throw new ServiceException("用户信息不完整", 400);
        }
        UserEmailApplyEntity oneByUsername = userMapper.findOneByUsername(user.getUserName());
        if (oneByUsername != null && oneByUsername.getId() != user.getId()) {
            throw new ServiceException("该用户名已存在",400);
        }
//        System.out.println(user.getId());
        UserEmailApplyEntity currentUserById = userMapper.getCurrentUserById(user.getId());
        if (currentUserById == null) {
            throw new ServiceException("用户不存在", 404);
        }
//        UserEmailApplyEntity oneByUsername = userMapper.findOneByUsername(user.getUserName());
        String oldUserName=currentUserById.getUserName();
        String newUserName=user.getUserName();
       if (!oldUserName.equals(newUserName)){
           int ownerRows = userMapper.updateUserNameInGroupOwner(oldUserName, newUserName);
           int friendRows = userMapper.updateUserNameInFriend(oldUserName, newUserName);
           int memberRows = userMapper.updateUserNameInGroupMember(oldUserName, newUserName);
           int messageRows=userMapper.updateUserNameInMessage(oldUserName,newUserName);
           int conversationRows=userMapper.updateUserNameInConversation(oldUserName,newUserName);
       }
       int result = userMapper.updateUser(user);

        // 🧹 手动清除缓存
        evictUserCaches(user.getId());
        cacheClient.evict("users:path", oldUserName);
        cacheClient.evict("users:path", newUserName);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateEmail(UpdateEmailDto dto) {
        // 1. 校验新邮箱是否已经被其他账号占用了
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", dto.getNewEmail());
        if (userMapper.selectCount(queryWrapper) > 0) {
            return Result.error("该邮箱已被其他账号绑定，请更换邮箱");
        }

        // 2. 查询当前用户是否存在
        int currentId = Math.toIntExact(BaseContext.getCurrentId());
        UserEmailApplyEntity currentUser = userMapper.getCurrentUserById(currentId);
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        // 3. 核心安全校验：校验账号密码
        if (!passwordEncoder.matches(dto.getPassword(), currentUser.getPassword())) {
            return Result.error("账号密码错误，无法修改邮箱");
        }

        // 4. 核心业务校验：校验新邮箱的验证码 (调用工具类，内部已自动清除使用过的验证码)
        boolean flag = mailUtil.verifyCode(dto.getNewEmail(), dto.getCode());
        if (!flag) {
            return Result.error("验证码错误或已过期");
        }

        // 5. 执行更新
        currentUser.setEmail(dto.getNewEmail());
        userMapper.updateEmailById(currentUser);

        // 6. 清除用户相关缓存
        evictUserCaches(currentId);

        return Result.success("邮箱绑定成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importUsers(List<UserImportDTO> dtoList) {
        List<UserEmailApplyEntity> insertList = new ArrayList<>();

        // 🌟 性能优化：在循环外只加密一次默认密码
        String defaultPassword = passwordEncoder.encode("123456");
        String defaultAvatar = "http://localhost:8000/file/55b8a0ad0e664e5bbf290f737dd29f49.jpg";

        for (UserImportDTO dto : dtoList) {
            UserEmailApplyEntity user = new UserEmailApplyEntity();

            // 1. 注入 Excel 解析的数据
            user.setUserName(dto.getUserName());
            user.setEmail(dto.getEmail());
            user.setRole(dto.getRole() != null ? dto.getRole() : 2); // 没填默认普通用户(2)

            // 2. 注入系统强制默认值
            user.setPassword(defaultPassword);
            user.setPath(defaultAvatar);
            user.setStatus(1); // 默认状态正常

            insertList.add(user);
        }

        // 3. 调用 MyBatis 原生批量插入方法
        // 注意：如果数据量极大(上万条)，建议在 Service 拆分成每 1000 条插入一次，防止 SQL 过长报错。
        if (!insertList.isEmpty()) {
            userMapper.insertBatch(insertList);
            evictUserCaches(null);
        }
    }

    @Override
    public Result login(User user, Integer applyRole) {
//        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User loginUser = userMapper.findByUsername(user.getUserName());
        if (loginUser == null) throw new ServiceException("用户名或密码错误", 401);

        if (!passwordEncoder.matches(user.getPassword(), loginUser.getPassword())) {
            return Result.error("用户名或密码错误");
        } else {
            // 生成双 Token
            String accessToken = JWTUtil.generateToken(loginUser.getId(), loginUser.getUserName());
            String refreshToken = JWTUtil.generateRefreshToken(loginUser.getId(), loginUser.getUserName());

            UserEntity userEntity = new UserEntity();
            String email = userMapper.findEmailByUsername(loginUser.getUserName());
            BeanUtils.copyProperties(loginUser, userEntity);
            UserVo userVo = new UserVo(accessToken, refreshToken, userEntity, email);

            // ✅ 将 refreshToken 存入 Redis
            redisUtil.set("refreshToken:userId:" + loginUser.getId(), refreshToken, 7, TimeUnit.DAYS);

            return Result.success(userVo);
        }
    }

    @Override
    public List<User> findAllUsers() {
        return cacheClient.queryWithMutex(
                "users:all", "all", new TypeReference<List<User>>() {},
                ignored -> {
                    List<User> users = userMapper.findAllUsers();
                    return users == null ? new ArrayList<>() : users;
                }, 10, TimeUnit.MINUTES);
    }

    @Override
    public String getPath(String username) {
        return cacheClient.queryWithMutex("users:path", username, String.class,
                userMapper::getPath, 30, TimeUnit.MINUTES);
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        if (!JWTUtil.verifyToken(refreshToken)) {
            throw new ServiceException("登录已失效，请重新登录", 401);
        }

        int userId = JWTUtil.getUserId(refreshToken);
        String redisRt = redisUtil.get("refreshToken:userId:" + userId);
        if (redisRt == null || !redisRt.equals(refreshToken)) {
            throw new ServiceException("账号已在其他设备登录或被强制下线", 401);
        }

        String username = JWTUtil.getUsername(refreshToken);
        User user = new User().builder().id(userId).userName(username).build();

        String accessToken = JWTUtil.generateToken(user.getId(), user.getUserName());
        String newRefreshToken = JWTUtil.generateRefreshToken(user.getId(), user.getUserName());

        redisUtil.set("refreshToken:userId:" + userId, newRefreshToken, 7, TimeUnit.DAYS);

        Map<String, String> map = new HashMap<>();
        map.put("accessToken", accessToken);
        map.put("refreshToken", newRefreshToken);
        return map;
    }

    @Override
    public List<UserEmailApplyEntity> getPendingAdmins() {
        return cacheClient.queryWithMutex("users:pending-admins", "all",
                new TypeReference<List<UserEmailApplyEntity>>() {},
                ignored -> {
                    List<UserEmailApplyEntity> users = userMapper.selectPendingAdminList();
                    return users == null ? new ArrayList<>() : users;
                }, 2, TimeUnit.MINUTES);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveAdmin(Integer userId) {
        boolean updated = userMapper.approveAdmin(userId) > 0;
        if (updated) {
            evictUserCaches(userId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectAdmin(Integer userId) {
        boolean updated = userMapper.rejectAdmin(userId) > 0;
        if (updated) {
            evictUserCaches(userId);
        }
        return updated;
    }

    @Override
    public UserEmailApplyEntity getCurrentUserById(int currentId) {
        return cacheClient.queryWithMutex("users:detail", currentId,
                UserEmailApplyEntity.class, userMapper::getCurrentUserById,
                10, TimeUnit.MINUTES);
    }

    @Override
    public List<UserExportDTO> export(UserQueryExportDTO userQueryExportDTO) {
        List<UserEmailApplyEntity> userList = userMapper.export(userQueryExportDTO);
        List<UserExportDTO> exportDTOList = new ArrayList<>();

        for (UserEmailApplyEntity user : userList) {
            UserExportDTO dto = new UserExportDTO();
            dto.setId(user.getId());
            dto.setUserName(user.getUserName());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole());

//            String rawPath = user.getPath();
//            if (rawPath != null && !rawPath.trim().isEmpty()) {
//                try {
//                    dto.setPath(new java.net.URL(rawPath));
//                } catch (MalformedURLException e) {
//                    log.error("用户 [{}] 的头像路径格式非法: {}", user.getUserName(), rawPath);
//                    dto.setPath(null);
//                }
//            } else {
//                dto.setPath(null);
//            }
            exportDTOList.add(dto);
        }
        return exportDTOList;
    }

    @Override
    public void updatePassword(UserUpdatePasswordDTO userUpdatePasswordDTO) {
        Long currentId = BaseContext.getCurrentId();
        User currentUser = userMapper.findById(Math.toIntExact(currentId));
        if (currentUser == null) {
            throw new ServiceException("用户不存在", 400);
        }

        if (!passwordEncoder.matches(userUpdatePasswordDTO.getOldPassword(), currentUser.getPassword())) {
            throw new ServiceException("原密码输入错误", 400);
        }

        if (passwordEncoder.matches(userUpdatePasswordDTO.getNewPassword(), currentUser.getPassword())) {
            throw new ServiceException("新密码不能与原密码相同", 400);
        }

        String encodedNewPassword = passwordEncoder.encode(userUpdatePasswordDTO.getNewPassword());

        UserEntity updateUser = new UserEntity();
        updateUser.setId(Math.toIntExact(currentId));
        updateUser.setPassword(encodedNewPassword);

        redisUtil.delete("refreshToken:userId:" + currentId);
        userMapper.updateUser(updateUser);
        evictUserCaches(Math.toIntExact(currentId));
    }

    @Override
    public User findByToken(String token) {
        return userMapper.findByToken(token);
    }

    @Override
    public User findById(int id) {
        // 1. 🛡️ 布隆过滤器
        RBloomFilter<Integer> bloomFilter = redissonClient.getBloomFilter(BloomConstant.USER_BLOOM_FILTER);
        if (!bloomFilter.contains(id)) {
            log.warn("🛡️ [布隆过滤器] 拦截非法用户查询，ID: {}", id);
            return null;
        }

        // 2. 🚀 统一缓存：空值、互斥锁、随机 TTL 均由 CacheClient 处理
        return cacheClient.queryWithMutex("users:id", id, User.class,
                userMapper::findById, 2, TimeUnit.HOURS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int register(UserEmailDto userEmailDto, Integer applyRole) {
        UserEmailApplyEntity userEmailApplyEntity1 = userMapper.selectUserByEmail(userEmailDto.getEmail());
        if (userEmailApplyEntity1 != null) {
            throw new ServiceException("该邮箱用户已存在", 400);
        }

        // 1. 邮箱验证码校验
        String email = userEmailDto.getEmail();
        if (email != null) {
            // MailUtil 校验成功后会自动删除验证码缓存
            boolean flag = mailUtil.verifyCode(email, userEmailDto.getCode());
            if (!flag) {
                throw new ServiceException("验证码错误或已过期", 400);
            }
        }

        // 2. 校验用户名是否已存在
        User byUsername = userMapper.findByUsername(userEmailDto.getUserName());
        if (byUsername != null) throw new ServiceException("用户名已存在", 400);

        // 3. 构建用户实体并拷贝基础属性
        UserEmailApplyEntity userEmailApplyEntity = new UserEmailApplyEntity();
        BeanUtils.copyProperties(userEmailDto, userEmailApplyEntity);

        // 4. 角色与状态分配
        //申请普通用户：2，设置状态1：通过，权限：2
        if (applyRole != null && applyRole.equals(2)) {
            userEmailApplyEntity.setRole(2);
            userEmailApplyEntity.setStatus(1);
        } else {
            userEmailApplyEntity.setRole(1);
            userEmailApplyEntity.setStatus(0);
        }

        if (userEmailApplyEntity.getPath() == null || userEmailApplyEntity.getPath().isEmpty()) {
            userEmailApplyEntity.setPath("http://localhost:8000/default_avatar.png");
        }

        userEmailApplyEntity.setPassword(passwordEncoder.encode(userEmailApplyEntity.getPassword()));

        // 5. 执行数据库插入
        int result = userMapper.register(userEmailApplyEntity);

        // 6. 同步加入布隆过滤器
        Integer newUserId = userEmailApplyEntity.getId();
        if (newUserId != null) {
            RBloomFilter<Integer> bloomFilter = redissonClient.getBloomFilter("user:bloom:filter");
            bloomFilter.add(newUserId);
            log.info("新注册用户 ID {} 已同步加入布隆过滤器", newUserId);
        }

        evictUserCaches(newUserId);

        return 1;
    }

    // ======== 1. 邮箱登录 ========
    @Override
    public Result loginByEmail(EmailLoginDto dto) {
        // 1. 校验验证码 (工具类会自动清除 Redis 中的 code)
        boolean isCodeValid = mailUtil.verifyCode(dto.getEmail(), dto.getCode());
        if (!isCodeValid) {
            return Result.error("验证码错误或已过期");
        }

        // 2. 根据邮箱查找用户
        UserEmailApplyEntity user = userMapper.selectUserByEmail(dto.getEmail());

        if (user == null) {
            return Result.error("该邮箱未绑定任何账号");
        }

        // 3. 校验状态与角色
        if (user.getStatus() == 0) return Result.error("账号正在审核中，请耐心等待");
        if (user.getStatus() == 2) return Result.error("账号已被封禁");

        // 4. 生成双 Token
        String accessToken = JWTUtil.generateToken(user.getId(), user.getUserName());
        String refreshToken = JWTUtil.generateRefreshToken(user.getId(), user.getUserName());

        UserEntity userEntity = new UserEntity();
        String email = userMapper.findEmailByUsername(user.getUserName());
        BeanUtils.copyProperties(user, userEntity);
        UserVo userVo = new UserVo(accessToken, refreshToken, userEntity, email);

        // ✅ 将 refreshToken 存入 Redis
        redisUtil.set("refreshToken:userId:" + user.getId(), refreshToken, 7, TimeUnit.DAYS);

        return Result.success(userVo);
    }

    // ======== 2. 忘记密码 ========
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result forgotPassword(ForgotPasswordDto dto) {
        // 1. 校验验证码 (工具类会自动清除 Redis 中的 code)
        boolean isCodeValid = mailUtil.verifyCode(dto.getEmail(), dto.getCode());
        if (!isCodeValid) {
            return Result.error("验证码错误或已过期");
        }

        // 2. 根据邮箱查找用户
        UserEmailApplyEntity user = userMapper.selectUserByEmail(dto.getEmail());

        if (user == null) {
            return Result.error("该邮箱尚未注册或未绑定任何账号");
        }

        // 3. 修改密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updatePasswordById(user);
        evictUserCaches(user.getId());

        return Result.success("密码重置成功，请使用新密码登录");
    }

    @Override
    public String getPathByEmail(String email) {
        return cacheClient.queryWithMutex("users:path-email", email, String.class,
                userMapper::getPathByEmail, 30, TimeUnit.MINUTES);
    }



// ...


    @Override
    @Transactional(rollbackFor = Exception.class) // 🌟 涉及到多表修改，千万别忘了加事务！
    public void updateUsernameInGroupAndFriend(String oldUserName, String newUserName) {

        // 如果新老名字一样，直接 return，这才是最该在代码层拦截的“判断”
        if (oldUserName.equals(newUserName)) {
            return;
        }

        // 直接执行更新，MyBatis 会返回受影响的行数
        int ownerRows = userMapper.updateUserNameInGroupOwner(oldUserName, newUserName);
        int friendRows = userMapper.updateUserNameInFriend(oldUserName, newUserName);
        int memberRows = userMapper.updateUserNameInGroupMember(oldUserName, newUserName);
        int messageRows=userMapper.updateUserNameInMessage(oldUserName,newUserName);
        int conversationRows=userMapper.updateUserNameInConversation(oldUserName,newUserName);
        // 🌟 打印日志：通过返回值，我们就能清楚地知道到底有没有数据被修改
        log.info("【用户改名联级更新】[{}] 改为 [{}]: 更新群主表 {} 行, 好友表 {} 行, 群成员表 {} 行",
                oldUserName, newUserName, ownerRows, friendRows, memberRows);
        cacheClient.evict("users:path", oldUserName);
        cacheClient.evict("users:path", newUserName);
        cacheClient.evictKey(cacheClient.buildKey("users:all", "all"));
    }

    private String userQueryKey(UserQueryDTO query) {
        return "page=" + query.getPage()
                + ",size=" + query.getPageSize()
                + ",id=" + String.valueOf(query.getId())
                + ",name=" + String.valueOf(query.getUserName())
                + ",role=" + String.valueOf(query.getRole())
                + ",email=" + String.valueOf(query.getEmail());
    }

    private void evictUserCaches(Integer userId) {
        if (userId != null) {
            cacheClient.evict("users:id", userId);
            cacheClient.evict("users:detail", userId);
        }
        cacheClient.evictByPrefix("manage:users:apply");
        cacheClient.evictByPrefix("manage:users:append");
        cacheClient.evictKey(cacheClient.buildKey("users:all", "all"));
        cacheClient.evictKey(cacheClient.buildKey("users:pending-admins", "all"));
    }
}
