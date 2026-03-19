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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;


@Service("messageService")
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;


    private final UserMapper userMapper;
    private final MessageStructMapper messageStructMapper;


    @Override
    public Integer addMessage(MessageInfo messageInfo) {
        messageMapper.addMessage(messageInfo);

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
        } catch (Exception e) {
            throw new ServiceException("撤回消息失败", 500);
        }
    }

    @Override
    //删除unless = "#result == null"，防止缓存穿透
    @Cacheable(
            value = RedisKeyConstants.MANAGE_MESSAGES_CACHE,
            // 🌟 完美解法：加上 :list 作为真正的文件名，绝不以冒号结尾！
            key = "'page=1:pageSize=9:list'",
            condition = "#messageQueryDTO.page == 1 " +
                    "&& #messageQueryDTO.pageSize == 9 " +
                    "&& #messageQueryDTO.id == null " +
                    "&& (#messageQueryDTO.message == null || #messageQueryDTO.message == '') " +
                    "&& (#messageQueryDTO.fromName == null || #messageQueryDTO.fromName == '') " +
                    "&& (#messageQueryDTO.toName == null || #messageQueryDTO.toName == '') " +
                    "&& #messageQueryDTO.sendAtStart == null " +
                    "&& #messageQueryDTO.sendAtEnd == null"
    )
    public PageResult<List<Message>> pageQuery(MessageQueryDTO messageQueryDTO) {
        log.info("messageQueryDTO,{}", messageQueryDTO);
        //调用pagehelper的startPage方法传入page（当前页）和pageSize（每页显示数量）开启分页
        PageHelper.startPage(messageQueryDTO.getPage(), messageQueryDTO.getPageSize());
        //执行查询逻辑，获取分页结果集，使用pagehelper内置的page对象接收结果集
        Page<Message> page = messageMapper.pageQuery(messageQueryDTO);
        //从page对象钟获取total和list
        long total = page.getTotal();
        List<Message> result = page.getResult();
        //封装返回结果集
        return PageResult.success(total, result);
    }

    @Override
    @CacheEvict(value = RedisKeyConstants.MANAGE_MESSAGES_CACHE,
            // 🌟 完美解法：加上 :list 作为真正的文件名，绝不以冒号结尾！
            key = "'page=1:pageSize=9:list'")
    public void deleteMessage(Integer id) {
        try {
            messageMapper.deleteMessage(id);
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
