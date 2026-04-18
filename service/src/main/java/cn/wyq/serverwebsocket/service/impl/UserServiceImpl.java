package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.constant.BloomConstant;
import cn.wyq.serverwebsocket.constant.RedisKeyConstants;
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
import cn.wyq.serverwebsocket.utils.BaseContext;
import cn.wyq.serverwebsocket.utils.JWTUtil;
import cn.wyq.serverwebsocket.utils.MailUtil;
import cn.wyq.serverwebsocket.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
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



    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatGroupMapper chatGroupMapper;
    private final MessageMapper messageMapper;
    private final FriendMapper friendMapper;
    private final RedissonClient redissonClient;


    @Override
    @SuppressWarnings("unchecked")
    public PageResult<List<UserEntity>> userList(UserQueryDTO userQueryDTO) {
        // 1. 提取缓存条件判断
        boolean isCacheable = (userQueryDTO.getId() == null || String.valueOf(userQueryDTO.getId()).isEmpty())
                && userQueryDTO.getPage() == 1
                && userQueryDTO.getPageSize() == 9
                && (userQueryDTO.getUserName() == null || userQueryDTO.getUserName().isEmpty())
                && (userQueryDTO.getRole() == null || userQueryDTO.getRole() == 0);

        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";

        // 2. 查缓存
        if (isCacheable) {
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                log.debug("命中用户首页分页缓存！");
                return (PageResult<List<UserEntity>>) cachedData;
            }
        }

        // 3. 缓存未命中，查数据库
        PageHelper.startPage(userQueryDTO.getPage(), userQueryDTO.getPageSize());
        Page<UserEntity> page = userMapper.selectAllUsers(userQueryDTO);
        long total = page.getTotal();
        List<UserEntity> userList = page.getResult();
        PageResult<List<UserEntity>> pageResult = PageResult.success(total, userList);

        // 4. 🛡️ 核心防御：写缓存并防穿透
        if (isCacheable) {
            if (userList == null || userList.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, pageResult, 1, TimeUnit.MINUTES);
                log.info("用户列表为空，已存入空缓存防穿透");
            } else {
                redisTemplate.opsForValue().set(cacheKey, pageResult, 2, TimeUnit.HOURS);
            }
        }
        return pageResult;
    }

    @Override
    public PageResult<List<UserEntity>> userAppendList(UserQueryDTO userQueryDTO) {
        // 1. 提取缓存条件判断
        boolean isCacheable = (userQueryDTO.getId() == null || String.valueOf(userQueryDTO.getId()).isEmpty())
                && userQueryDTO.getPage() == 1
                && userQueryDTO.getPageSize() == 9
                && (userQueryDTO.getUserName() == null || userQueryDTO.getUserName().isEmpty())
                && (userQueryDTO.getRole() == null || userQueryDTO.getRole() == 0);

        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";

        // 2. 查缓存
        if (isCacheable) {
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                log.debug("命中用户首页分页缓存！");
                return (PageResult<List<UserEntity>>) cachedData;
            }
        }

        // 3. 缓存未命中，查数据库
        PageHelper.startPage(userQueryDTO.getPage(), userQueryDTO.getPageSize());
        Page<UserEntity> page = userMapper.selectAllAppendUsers(userQueryDTO);
        long total = page.getTotal();
        List<UserEntity> userList = page.getResult();
        PageResult<List<UserEntity>> pageResult = PageResult.success(total, userList);

        // 4. 🛡️ 核心防御：写缓存并防穿透
        if (isCacheable) {
            if (userList == null || userList.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, pageResult, 1, TimeUnit.MINUTES);
                log.info("用户列表为空，已存入空缓存防穿透");
            } else {
                redisTemplate.opsForValue().set(cacheKey, pageResult, 2, TimeUnit.HOURS);
            }
        }
        return pageResult;
    }

    @Override
    public Result searchUser(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error("搜索关键词不能为空");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("user_name", keyword);
        queryWrapper.select("id", "user_name", "path");
        queryWrapper.last("LIMIT 20");

        List<User> userList = userMapper.selectList(queryWrapper);

        if (userList.isEmpty()) {
            return Result.success("未搜索到相关用户");
        }
        return Result.success(userList);
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
        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";
        redisTemplate.delete(cacheKey);
        // 清理用户详情缓存
        redisTemplate.delete("user:id:" + id);
        // 🌟 强制将该用户踢下线
        redisTemplate.delete("refreshToken:userId:" + id);

        return result;
    }

    @Override
    public int updateUser(UserEntity user) {
        int result = userMapper.updateUser(user);

        // 🧹 手动清除缓存
        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";
        redisTemplate.delete(cacheKey);
        String cacheKey1 = "user:id:" + user.getId();
        redisTemplate.delete(cacheKey1);
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

        // 6. 清除用户列表缓存
        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";
        redisTemplate.delete(cacheKey);

        return Result.success("邮箱绑定成功");
    }

    @Override
    public Result login(User user, Integer applyRole) {
        User loginUser = userMapper.login(user, applyRole);
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
        return userMapper.findAllUsers();
    }

    @Override
    public String getPath(String username) {
        return userMapper.getPath(username);
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
        return userMapper.selectPendingAdminList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveAdmin(Integer userId) {
        // 🧹 清除缓存
        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";
        redisTemplate.delete(cacheKey);

        return userMapper.approveAdmin(userId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectAdmin(Integer userId) {
        return userMapper.rejectAdmin(userId) > 0;
    }

    @Override
    public UserEmailApplyEntity getCurrentUserById(int currentId) {
        return userMapper.getCurrentUserById(currentId);
    }

    @Override
    public List<UserExportDTO> export(UserQueryExportDTO userQueryExportDTO) {
        List<UserEntity> userList = userMapper.export(userQueryExportDTO);
        List<UserExportDTO> exportDTOList = new ArrayList<>();

        for (UserEntity user : userList) {
            UserExportDTO dto = new UserExportDTO();
            dto.setId(user.getId());
            dto.setUserName(user.getUserName());
            dto.setRole(user.getRole());

            String rawPath = user.getPath();
            if (rawPath != null && !rawPath.trim().isEmpty()) {
                try {
                    dto.setPath(new java.net.URL(rawPath));
                } catch (MalformedURLException e) {
                    log.error("用户 [{}] 的头像路径格式非法: {}", user.getUserName(), rawPath);
                    dto.setPath(null);
                }
            } else {
                dto.setPath(null);
            }
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

        redisTemplate.delete("refreshToken:userId:" + currentId);
        userMapper.updateUser(updateUser);
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

        // 2. 🚀 Redis 缓存
        String cacheKey = "user:id:" + id;
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            if ("".equals(cachedData)) {
                log.debug("[Redis空值缓存] 拦截非法查询，ID: {}", id);
                return null;
            }
            return (User) cachedData;
        }

        // 3. 🔐 分布式锁
        String lockKey = "lock:user:" + id;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                cachedData = redisTemplate.opsForValue().get(cacheKey);
                if (cachedData != null) {
                    return "".equals(cachedData) ? null : (User) cachedData;
                }

                User user = userMapper.findById(id);
                if (user == null) {
                    redisTemplate.opsForValue().set(cacheKey, "", 1, TimeUnit.MINUTES);
                    log.info("[数据库空结果] ID: {} 为误判，已存入空值缓存", id);
                } else {
                    redisTemplate.opsForValue().set(cacheKey, user, 2, TimeUnit.HOURS);
                    log.info("[数据库查询成功] 已更新缓存，ID: {}", id);
                }
                return user;
            } else {
                log.error("❌ [分布式锁] 获取锁超时，ID: {}", id);
                throw new ServiceException("服务器忙，请稍后再试", 503);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("系统异常", 500);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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

        // 7. 手动清除列表缓存
        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";
        redisTemplate.delete(cacheKey);

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

        return Result.success("密码重置成功，请使用新密码登录");
    }

    @Override
    public String getPathByEmail(String email) {
        return userMapper.getPathByEmail(email);
    }
}