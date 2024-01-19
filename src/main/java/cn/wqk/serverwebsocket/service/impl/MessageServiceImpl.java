package cn.wqk.serverwebsocket.service.impl;

import cn.wqk.serverwebsocket.mapper.MessageMapper;
import cn.wqk.serverwebsocket.service.MessageService;
import cn.wqk.serverwebsocket.socket.pojo.MessageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("messageService")
public class MessageServiceImpl implements MessageService {
    @Autowired
    private MessageMapper messageMapper;



    @Override
    public Integer addMessage(MessageInfo messageInfo) {
        return messageMapper.addMessage(messageInfo);
    }

    @Override
    public List<MessageInfo> findAll(MessageInfo messageInfo) {
        return messageMapper.findAll(messageInfo);
    }
}
