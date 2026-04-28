package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.constant.RedisKeyConstants;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.ChatGroupMapper;
import cn.wyq.serverwebsocket.mapper.MessageMapper;
import cn.wyq.serverwebsocket.mapper.MessageStructMapper;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.dto.MessageExportDTO;
import cn.wyq.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.pojo.socket.MessageFull;
import cn.wyq.serverwebsocket.pojo.socket.MessageInfo;
import cn.wyq.serverwebsocket.service.MessageService;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Service("messageService")
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final MessageStructMapper messageStructMapper;
    private final ChatGroupMapper chatGroupMapper;


    // 💡 核心新增：注入 RedisTemplate
    // 由于使用了 @RequiredArgsConstructor，声明为 private final 即可自动注入
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Result clearHistory(String userName, String targetName) {
        // 直接调用 Mapper 删除两人之间的所有消息
        int rows = messageMapper.deleteChatHistory(userName, targetName);
        return Result.success("聊天记录已清空");
    }

    @Override
    public Result getFilteredHistory(String userName, String targetName, Integer type, String keyword) {
        // 🚨 注意：如果你的数据库底层实体类叫 MessageInfo，请将泛型换成 MessageInfo
        QueryWrapper<Message> query = new QueryWrapper<>();

        // 1. 确定聊天对象（群聊 or 私聊）
        if (targetName.startsWith("GROUP_")) {
            query.eq("to_name", targetName);
        } else {
            query.and(wrapper -> wrapper
                    .eq("from_name", userName).eq("to_name", targetName)
                    .or()
                    .eq("from_name", targetName).eq("to_name", userName)
            );
        }

        // 2. 根据消息类型进行前缀筛选
        // 0=全部, 1=纯文本, 2=图片, 3=文件
        if (type != null) {
            if (type == 1) {
                // 纯文本：既不是图片也不是文件
                query.notLikeRight("message", "[IMAGE]").notLikeRight("message", "[FILE]");
            } else if (type == 2) {
                // 图片：以 [IMAGE] 开头
                query.likeRight("message", "[IMAGE]");
            } else if (type == 3) {
                // 文件：以 [FILE] 开头
                query.likeRight("message", "[FILE]");
            }
        }

        // 3. 关键词模糊搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.like("message", keyword.trim());
        }

        // 4. 按时间倒序（最新的聊天记录排在最前面展示）
        // 🚨 注意：如果你的数据库时间字段叫 time，请换成 "time"；如果是 send_at 就保持不变
        query.orderByDesc("send_at");

        // 5. 执行查询
        List<Message> dbMessages = messageMapper.selectList(query);

        // 6. 转换为前端需要的 MessageFull 格式
        List<MessageFull> finalMessageList = new ArrayList<>();
        for (Message dbMsg : dbMessages) {
            MessageFull info = new MessageFull();
            BeanUtils.copyProperties(dbMsg, info);
            finalMessageList.add(info);
        }

        // 7. 补充发送者和接收者的头像信息
        if (!finalMessageList.isEmpty()) {
            for (MessageFull full : finalMessageList) {
                if (full.getFromName() != null) {
                    full.setFromPath(userMapper.getStatusAndPathByUserName(full.getFromName()));
                }
                if (full.getToName() != null && !full.getToName().startsWith("GROUP_")) {
                    full.setToPath(userMapper.getStatusAndPathByUserName(full.getToName()));
                }
            }
        }

        return Result.success(finalMessageList);
    }

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


    @Override
    public MessageFull recallMessage(int id) {
        // 1. 查出原消息
        MessageInfo oldMsg = messageMapper.checkTime(id);


        // 2. 超时判断

        if (oldMsg == null) {
            throw new ServiceException("消息发送超过两分钟不能撤回", 400);
        }

        try {
            // 3. 数据库将消息内容置空/打上撤回标记
            messageMapper.recallMessage(id);

            // 4. 组装一条空消息对象并返回给 Controller
            MessageFull recallEvent = new MessageFull();
            recallEvent.setId(id);
            recallEvent.setMessage(""); // 传空字符串，前端识别为撤回
            recallEvent.setFromName(oldMsg.getFromName());
            recallEvent.setToName(oldMsg.getToName());
            recallEvent.setTime(oldMsg.getTime());

            // 🌟 核心：直接返回，Service 层绝对不碰 WebSocket！
            return recallEvent;

        } catch (Exception e) {
            log.error("撤回消息异常", e);
            throw new ServiceException("撤回消息失败", 500);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageResult<List<Message>> pageQuery(MessageQueryDTO messageQueryDTO) {
        // 3. 缓存未命中（或带条件查询），执行数据库检索
        PageHelper.startPage(messageQueryDTO.getPage(), messageQueryDTO.getPageSize());
        Page<Message> page = messageMapper.pageQuery(messageQueryDTO);

        long total = page.getTotal();
        List<Message> result = page.getResult();
        for (Message message : result) {
            if (message.getToName().startsWith("GROUP")) {
                String groupName = chatGroupMapper.findGroupNameByGroupId(message.getToName());
                message.setToName("群聊："+groupName);
            }
        }
        PageResult<List<Message>> pageResult = PageResult.success(total, result);
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

    @Override
    public void exportMessages(MessageQueryDTO messageQueryDTO, HttpServletResponse response) throws IOException {
        // 1. 直接全量查询，不走分页
        List<Message> list = messageMapper.selectListByCondition(messageQueryDTO);

        // 2. 将查出来的实体集合转换为导出的 DTO 集合，并处理群聊格式
        List<MessageExportDTO> exportList = new ArrayList<>();
        for (Message message : list) {
            MessageExportDTO dto = new MessageExportDTO();
            // 拷贝基础属性 (id, message, fromName, sendAt)
            BeanUtils.copyProperties(message, dto);

            // 单独处理 toName (群聊逻辑保持和你原来的一模一样)
            if (message.getToName() != null && message.getToName().startsWith("GROUP")) {
                String groupName = chatGroupMapper.findGroupNameByGroupId(message.getToName());
                dto.setToName("群聊：" + groupName);
            } else {
                dto.setToName(message.getToName());
            }

            exportList.add(dto);
        }

        // 3. 设置 HTTP 响应头，触发浏览器下载
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("消息记录明细", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

        // 4. 使用 EasyExcel 将数据写入输出流
        EasyExcel.write(response.getOutputStream(), MessageExportDTO.class)
                .sheet("消息数据")
                .doWrite(exportList);
    }
}