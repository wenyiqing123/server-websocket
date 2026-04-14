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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service("messageService")
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final MessageStructMapper messageStructMapper;

    // 💡 核心新增：注入 RedisTemplate
    // 由于使用了 @RequiredArgsConstructor，声明为 private final 即可自动注入
    private final RedisTemplate<String, Object> redisTemplate;


    // 你的方法名可能是 getMessageList 或类似的名字，找到处理历史消息查询的方法
//    @Override
//    public Result getMessageList(String fromName, String toName) {
//        QueryWrapper<MessageInfo> query = new QueryWrapper<>();
//
//        // 🌟 核心修复：如果是群聊，只认 "发给这个群" 的消息
//        if (toName.startsWith("GROUP_")) {
//            query.eq("to_name", toName);
//        }
//        // 🌟 私聊逻辑：A发给B，或者 B发给A
//        else {
//            query.and(wrapper -> wrapper
//                    .eq("from_name", fromName).eq("to_name", toName)
//                    .or()
//                    .eq("from_name", toName).eq("to_name", fromName)
//            );
//        }
//
//        // 按照时间排序
//        query.orderByAsc("time");
//
//        // 假设你的 mapper 是 messageInfoMapper
//        List<MessageInfo> list = messageMapper.selectList(query);
//        return Result.success(list);
//    }
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
        // 1. 声明一个统一的列表来接收最终的 MessageFull 数据
        List<MessageFull> finalMessageList = new ArrayList<>();

        // 2. 判断是群聊还是私聊，分别查出数据并转化为 MessageFull
        if (messageInfo.getToName() != null && messageInfo.getToName().startsWith("GROUP_")) {
            // 🌟 群聊逻辑：只认 "发给这个群" 的消息
            QueryWrapper<Message> query = new QueryWrapper<>();
            query.eq("to_name", messageInfo.getToName());
            query.orderByAsc("send_at"); // 按照时间排序

            List<Message> dbMessages = messageMapper.selectList(query);

            // 将底层实体类 Message 转化为 MessageFull
            for (Message dbMsg : dbMessages) {
                MessageFull info = new MessageFull();
                info.setTime(dbMsg.getSendAt());
                BeanUtils.copyProperties(dbMsg, info);
                finalMessageList.add(info);
            }
        } else {
            // 🌟 私聊逻辑：保持你原有的自定义 SQL 查询不变
            List<MessageInfo> messageInfoList = messageMapper.findAll(messageInfo);

            // 统一使用 MapStruct 进行批量基础属性拷贝
            List<MessageFull> mappedList = messageStructMapper.toFullList(messageInfoList);
            if (mappedList != null) {
                finalMessageList.addAll(mappedList);
            }
        }

        // 3. 🌟 核心修复：把查头像的逻辑移到 if-else 外面，保证群聊和私聊都能执行到！
        if (!finalMessageList.isEmpty()) {
            for (MessageFull full : finalMessageList) {

                // 【群聊和私聊都需要】：获取发送者头像（群里也要显示是谁发的）
                if (full.getFromName() != null) {
                    String fromPath = userMapper.getPath(full.getFromName());
                    full.setFromPath(fromPath);
                }

                // 【仅私聊需要】：获取接收者头像
                // (群聊时 toName 是 GROUP_xxx，去 user 表查会报错，所以必须拦截)
                if (full.getToName() != null && !full.getToName().startsWith("GROUP_")) {
                    String toPath = userMapper.getPath(full.getToName());
                    full.setToPath(toPath);
                }
            }
        }

        // 4. 返回最终拼装好头像的数据给前端
        return finalMessageList;
    }

//    @Override
//    public List<MessageFull> findAll(MessageInfo messageInfo) {
//        // 1. 获取原始数据列表
//        List<MessageInfo> messageInfoList = messageMapper.findAll(messageInfo);
//        System.out.println("messageInfoList = " + messageInfoList);
//        // 2. 使用 MapStruct 进行批量基础属性拷贝 (id, title, content 等)
//        List<MessageFull> messageFulls = messageStructMapper.toFullList(messageInfoList);
//
//        // 3. 补充需要查询数据库的字段
//        for (MessageFull full : messageFulls) {
//            // 获取发送者信息
//            full.setFromPath(userMapper.getStatusAndPathByUserName(full.getFromName()));
//            // 获取接收者信息
//            full.setToName(userMapper.getStatusAndPathByUserName(full.getToName()));
//        }
//
//        return messageFulls;
//    }

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

    @Override
    public void deleteMessageByUser(Integer id) {
        messageMapper.deleteMessageByUser(id);
    }
}