package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.constant.RedisKeyConstants;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.MessageMapper;
import cn.wyq.serverwebsocket.mapper.MessageStructMapper;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.dto.MessageExportDTO;
import cn.wyq.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.pojo.socket.MessageFull;
import cn.wyq.serverwebsocket.pojo.socket.MessageInfo;
import cn.wyq.serverwebsocket.service.MessageService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service("messageService")
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final MessageStructMapper messageStructMapper;

    // 💡 核心新增：注入 RedisTemplate
    // 由于使用了 @RequiredArgsConstructor，声明为 private final 即可自动注入
    private final RedisTemplate<String, Object> redisTemplate;


    @Override
    public Integer addMessage(MessageInfo messageInfo) {
        messageMapper.addMessage(messageInfo);

        // 💡 架构师温馨提示：发新消息会改变第一页的列表，建议在这里也加上这行代码清除缓存
        // String cacheKey = RedisKeyConstants.MANAGE_MESSAGES_CACHE + ":page=1:pageSize=9:list";
        // redisTemplate.delete(cacheKey);

        return messageInfo.getId();
    }

    @Override
    public List<MessageFull> findAll(MessageInfo messageInfo) {
        // 1. 获取原始数据列表
        List<MessageInfo> messageInfoList = messageMapper.findAll(messageInfo);

        // 2. 使用 MapStruct 进行批量基础属性拷贝 (id, title, content 等)
        List<MessageFull> messageFulls = messageStructMapper.toFullList(messageInfoList);

        // 3. 补充需要查询数据库的字段
        for (MessageFull full : messageFulls) {
            // 获取发送者信息
            full.setFromPath(userMapper.getStatusAndPathByUserName(full.getFromName()));
            // 获取接收者信息
            full.setToName(userMapper.getStatusAndPathByUserName(full.getToName()));
        }

        return messageFulls;
    }

    @Override
    public void recallMessage(int id) {
        //判断时间差是否大于两分钟
        MessageInfo messageInfo = messageMapper.checkTime(id);
        if (messageInfo != null) {
            throw new ServiceException("消息超过两分钟不能撤回", 400);
        }
        try {
            messageMapper.recallMessage(id);

            // 💡 架构师温馨提示：撤回消息也会改变第一页的列表，建议清除缓存
            // String cacheKey = RedisKeyConstants.MANAGE_MESSAGES_CACHE + ":page=1:pageSize=9:list";
            // redisTemplate.delete(cacheKey);

        } catch (Exception e) {
            throw new ServiceException("撤回消息失败", 500);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageResult<List<Message>> pageQuery(MessageQueryDTO messageQueryDTO) {
        log.info("messageQueryDTO,{}", messageQueryDTO);

        // 1. 将原注解中的复杂 condition 转换为清晰的 Java 逻辑判断
        // 只有在查询第一页，且没有任何筛选条件时，才走缓存逻辑
        boolean isCacheable = messageQueryDTO.getPage() == 1
                && messageQueryDTO.getPageSize() == 9
                && messageQueryDTO.getId() == null
                && (messageQueryDTO.getMessage() == null || messageQueryDTO.getMessage().isEmpty())
                && (messageQueryDTO.getFromName() == null || messageQueryDTO.getFromName().isEmpty())
                && (messageQueryDTO.getToName() == null || messageQueryDTO.getToName().isEmpty())
                && messageQueryDTO.getSendAtStart() == null
                && messageQueryDTO.getSendAtEnd() == null;

        // 拼装缓存 Key，保持和之前 Spring Cache 生成的 Key 格式绝对一致
        String cacheKey = RedisKeyConstants.MANAGE_MESSAGES_CACHE + ":page=1:pageSize=9:list";

        // 2. 如果满足缓存条件，尝试从 Redis 获取数据
        if (isCacheable) {
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                log.debug("命中首页消息分页缓存！");
                return (PageResult<List<Message>>) cachedData;
            }
        }

        // 3. 缓存未命中（或带条件查询），执行数据库检索
        PageHelper.startPage(messageQueryDTO.getPage(), messageQueryDTO.getPageSize());
        Page<Message> page = messageMapper.pageQuery(messageQueryDTO);

        long total = page.getTotal();
        List<Message> result = page.getResult();
        PageResult<List<Message>> pageResult = PageResult.success(total, result);

        // 4. 🛡️ 如果是首页查询，将结果写入 Redis (核心防穿透逻辑)
        if (isCacheable) {
            if (result == null || result.isEmpty()) {
                // 如果数据库为空（可能是新系统没数据，或遭遇恶意穿透）
                // 存入一个包含空 List 的 PageResult 对象，过期时间 1 分钟
                redisTemplate.opsForValue().set(cacheKey, pageResult, 1, TimeUnit.MINUTES);
                log.info("消息列表为空，已存入空缓存防穿透");
            } else {
                // 如果是正常数据，存入缓存，过期时间 2 小时
                redisTemplate.opsForValue().set(cacheKey, pageResult, 2, TimeUnit.HOURS);
            }
        }

        return pageResult;
    }

    @Override
    public void deleteMessage(Integer id) {
        try {
            // 1. 操作数据库
            messageMapper.deleteMessage(id);

            // 2. 🧹 手动清理 Redis 缓存 (替代原本的 @CacheEvict)
            String cacheKey = RedisKeyConstants.MANAGE_MESSAGES_CACHE + ":page=1:pageSize=9:list";
            redisTemplate.delete(cacheKey);
            log.debug("成功清除消息分页缓存");

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ServiceException("删除失败", 500);
        }
    }

    @Override
    public List<Message> export(MessageExportDTO messageExportDTO) {
        return messageMapper.export(messageExportDTO);
    }
}