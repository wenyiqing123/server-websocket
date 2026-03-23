package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.constant.BloomConstant;
import cn.wyq.serverwebsocket.constant.RedisKeyConstants;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.dto.*;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.pojo.vo.UserVo;
import cn.wyq.serverwebsocket.service.UserService;
import cn.wyq.serverwebsocket.utils.BaseContext;
import cn.wyq.serverwebsocket.utils.JWTUtil;
import cn.wyq.serverwebsocket.utils.MailUtil;
import cn.wyq.serverwebsocket.utils.RedisUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private MailUtil mailUtil;

    // 💡 核心新增：注入 RedisTemplate 用于手动精细化控制缓存
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    @SuppressWarnings("unchecked")
    public PageResult<List<UserEntity>> userList(UserQueryDTO userQueryDTO) {

        // 1. 提取缓存条件判断 (替代原 condition 注解)
        // 确保只有在查第一页且无任何筛选条件时，才走缓存
        boolean isCacheable = (userQueryDTO.getId() == null || String.valueOf(userQueryDTO.getId()).isEmpty()) &&
                userQueryDTO.getPage() == 1 &&
                userQueryDTO.getPageSize() == 9 &&
                (userQueryDTO.getUserName() == null || userQueryDTO.getUserName().isEmpty()) &&
                (userQueryDTO.getRole() == null || userQueryDTO.getRole() == 0);

        // 保持原本 Spring Cache 生成的 Key 格式完全一致
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
                // 若数据库为空，存入一个极短命 (1 分钟) 的空结果外壳
                redisTemplate.opsForValue().set(cacheKey, pageResult, 1, TimeUnit.MINUTES);
                log.info("用户列表为空，已存入空缓存防穿透");
            } else {
                // 正常数据，存入 2 小时
                redisTemplate.opsForValue().set(cacheKey, pageResult, 2, TimeUnit.HOURS);
            }
        }

        return pageResult;
    }

    @Override
    public int deleteUserById(Integer id) {
        int result = userMapper.deleteById(id);

        // 🧹 手动清除缓存 (替代 @CacheEvict)
        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";
        redisTemplate.delete(cacheKey);

        return result;
    }

    @Override
    public int updateUser(UserEntity user) {
        int result = userMapper.updateUser(user);

        // 🧹 手动清除缓存 (替代 @CacheEvict)
        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";
        redisTemplate.delete(cacheKey);

        return result;
    }

    @Override
    public Result login(User user) {
        User loginUser = userMapper.login(user);
        if (loginUser == null) throw new ServiceException("用户名或密码错误", 401);
        if (!passwordEncoder.matches(user.getPassword(), loginUser.getPassword())) {
            return Result.error("用户名或密码错误");
        } else {
            // 生成双 Token
            String accessToken = JWTUtil.generateToken(loginUser.getId(), loginUser.getUserName());
            String refreshToken = JWTUtil.generateRefreshToken(loginUser.getId(), loginUser.getUserName());

            UserEntity userEntity = new UserEntity();
            BeanUtils.copyProperties(loginUser, userEntity);
            UserVo userVo = new UserVo(accessToken, refreshToken, userEntity);

            // ✅ 将 refreshToken 存入 Redis，保留踢人或单点登录的控制权
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
        // 1. 从 ThreadLocal 获取当前登录用户的 ID
        Long currentId = BaseContext.getCurrentId();

        // 2. 根据 ID 去数据库查出当前用户的真实信息（核心是为了拿到数据库里的密文旧密码）
        // 这里假设你的 mapper 里有根据 id 查询的方法，叫 selectById 或 getById
        User currentUser = userMapper.findById(Math.toIntExact(currentId));
        if (currentUser == null) {
            throw new ServiceException("用户不存在",400); // 替换为你项目中的自定义异常类
        }

        // 3. 核心防线：校验旧密码是否正确
        // 前端传的是明文：userUpdatePasswordDTO.getOldPassword()
        // 数据库里是密文：currentUser.getPassword()
        if (!passwordEncoder.matches(userUpdatePasswordDTO.getOldPassword(), currentUser.getPassword())) {
            throw new ServiceException("原密码输入错误",400);
        }

        // 4. 体验优化：新密码不能和旧密码一样（可选）
        if (passwordEncoder.matches(userUpdatePasswordDTO.getNewPassword(), currentUser.getPassword())) {
            throw new ServiceException("新密码不能与原密码相同",400);
        }

        // 5. 对新密码进行加密
        String encodedNewPassword = passwordEncoder.encode(userUpdatePasswordDTO.getNewPassword());

        // 6. 构造更新实体 🚨（注意这里的“局部更新”思想）
        UserEntity updateUser = new UserEntity();
        updateUser.setId(Math.toIntExact(currentId));
        updateUser.setPassword(encodedNewPassword);
        // 如果你的表里有 updateTime 或者 updateUser 字段，且没有配置 MyBatis-Plus 自动填充，记得在这里手动 set 一下
        // updateUser.setUpdateTime(LocalDateTime.now());
        redisTemplate.delete("refreshToken:userId:"+currentId);
        // 7. 调用 mapper 执行更新
        userMapper.updateUser(updateUser);
    }

    @Override
    public User findByToken(String token) {
        return userMapper.findByToken(token);
    }

    @Override

    public User findById(int id) {
        // 1. 🛡️ 第一道防线：布隆过滤器 (防止缓存穿透)
        RBloomFilter<Integer> bloomFilter = redissonClient.getBloomFilter(BloomConstant.USER_BLOOM_FILTER);
        if (!bloomFilter.contains(id)) {
            log.warn("🛡️ [布隆过滤器] 拦截非法用户查询，ID: {}", id);
            return null;
        }
        // 2. 🚀 第二道防线：查询 Redis 缓存 (追求性能)
        String cacheKey = "user:id:" + id;
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        // 检查是否命中缓存（包括空值标记）
        if (cachedData != null) {
            if ("".equals(cachedData)) {
                log.debug("[Redis空值缓存] 拦截非法查询，ID: {}", id);
                return null;
            }
            return (User) cachedData;
        }
        // 3. 🔐 第三道防线：分布式锁 (防止缓存击穿)
        // 只有缓存没命中，且布隆过滤器认为数据【可能存在】时，才抢锁
        String lockKey = "lock:user:" + id;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试加锁：最多等待 5 秒，加锁后 10 秒自动释放（防止死锁）
            // Redisson 的看门狗机制会自动续期，所以 10s 是安全的兜底时间
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                // 🌟 核心：二次检查 (Double-Check)
                // 当 100 个线程排队时，第 1 个抢到锁的人写完缓存走了，
                // 剩下的 99 个人进来后，必须先看一眼缓存，防止重复冲击数据库。
                cachedData = redisTemplate.opsForValue().get(cacheKey);
                if (cachedData != null) {
                    return "".equals(cachedData) ? null : (User) cachedData;
                }
                // 4. 🗄️ 真正查数据库
                User user = userMapper.findById(id);
                if (user == null) {
                    // 再次确认：如果是布隆过滤器误判，则存入空值标记 (防止穿透)
                    redisTemplate.opsForValue().set(cacheKey, "", 1, TimeUnit.MINUTES);
                    log.info("[数据库空结果] ID: {} 为误判，已存入空值缓存", id);
                } else {
                    // 正常回写缓存 (设置 2 小时有效期)
                    redisTemplate.opsForValue().set(cacheKey, user, 2, TimeUnit.HOURS);
                    log.info("[数据库查询成功] 已更新缓存，ID: {}", id);
                }
                return user;
            } else {
                // 如果抢锁失败（等了5秒还没抢到），可以根据业务选择报错或重试
                log.error("❌ [分布式锁] 获取锁超时，ID: {}", id);
                throw new ServiceException("服务器忙，请稍后再试", 503);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("系统异常", 500);
        } finally {
            // 5. 🔓 释放锁 (务必放在 finally 中，且只释放自己持有的锁)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 🌟 重构：注册成功后，必须将新 ID 同步加入布隆过滤器
     */
    @Override
    public int register(UserEmailDto userEmailDto) {
        String code = userEmailDto.getCode();
        String email = userEmailDto.getEmail();
        boolean flag = mailUtil.verifyCode(email, code);
        if (!flag) throw new ServiceException("验证码错误", 400);

        User byUsername = userMapper.findByUsername(userEmailDto.getUserName());
        if (byUsername != null) {
            return 0;
        }

        User user = new User();
        BeanUtils.copyProperties(userEmailDto, user);

        // 执行数据库插入
        int result = userMapper.register(user);

        // 🌟 核心新增：获取刚生成的用户 ID (前提：MyBatis 的 insert 语句配置了主键返回)
        Integer newUserId = user.getId();

        // 将新 ID 加入布隆过滤器
        if (newUserId != null) {
            RBloomFilter<Integer> bloomFilter = redissonClient.getBloomFilter("user:bloom:filter");
            bloomFilter.add(newUserId);
            log.info("新注册用户 ID {} 已同步加入布隆过滤器", newUserId);
        }

        // 🧹 手动清除缓存 (注册了新用户，第一页的列表数据可能发生了变化)
        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";
        redisTemplate.delete(cacheKey);

        return result;
    }
}