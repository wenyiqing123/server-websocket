package cn.wqk.serverwebsocket.service.impl;

import cn.wqk.serverwebsocket.framework.common.PageResult;
import cn.wqk.serverwebsocket.framework.exception.ServiceException;
import cn.wqk.serverwebsocket.mapper.MessageMapper;
import cn.wqk.serverwebsocket.mapper.UserMapper;
import cn.wqk.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wqk.serverwebsocket.pojo.entity.Message;
import cn.wqk.serverwebsocket.service.MessageService;
import cn.wqk.serverwebsocket.socket.pojo.MessageFull;
import cn.wqk.serverwebsocket.socket.pojo.MessageInfo;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("messageService")
@Slf4j
public class MessageServiceImpl implements MessageService {
    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;


    @Override
    public Integer addMessage(MessageInfo messageInfo) {
        messageMapper.addMessage(messageInfo);

        return messageInfo.getId();
    }

    @Override
    public List<MessageFull> findAll(MessageInfo messageInfo) {
        List<MessageInfo> messageInfoList = messageMapper.findAll(messageInfo);
        List<MessageFull> messageFulls = new ArrayList<MessageFull>();
        for (MessageInfo info : messageInfoList) {
            MessageFull full = new MessageFull();
            BeanUtils.copyProperties(info, full); // id、title、content 自动拷贝
            //获取发送者的信息
            String fromPath = userMapper.getStatusAndPathByUserName(info.getFromName());
            full.setFromPath(fromPath);
//            full.setFromStatus(statusAndPathByUserName.getStatus());
            //获取接收者的信息
            String toPath = userMapper.getStatusAndPathByUserName(info.getToName());
            full.setToName(toPath);
//            full.setToStatus(statusAndPathByUserName1.getStatus()); // MessageFull 特有字段
            messageFulls.add(full);
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
    public PageResult<List<Message>> pageQuery(MessageQueryDTO messageQueryDTO) {
        //调用pagehelper的startPage方法传入page（当前页）和pageSize（每页显示数量）开启分页
        PageHelper.startPage(messageQueryDTO.getPage(), messageQueryDTO.getPageSize());
        //执行查询逻辑，获取分页结果集，使用pagehelper内置的page对象接收结果集
        Page<Message> page = messageMapper.pageQuery(messageQueryDTO);
        //从page对象钟获取total和list
        long total = page.getTotal();
        List<Message> result = page.getResult();
        System.out.println("result = " + result);
        //封装返回结果集
        return PageResult.success(total, result);
    }

    @Override
    public void deleteMessage(Integer id) {
        try {
            messageMapper.deleteMessage(id);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ServiceException("删除失败", 500);
        }
    }
}
