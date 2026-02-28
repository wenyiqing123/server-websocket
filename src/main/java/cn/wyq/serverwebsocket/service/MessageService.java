package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.framework.common.PageResult;
import cn.wyq.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.socket.pojo.MessageFull;
import cn.wyq.serverwebsocket.socket.pojo.MessageInfo;

import java.util.List;

public interface MessageService {
    Integer addMessage(MessageInfo messageInfo);

    List<MessageFull> findAll(MessageInfo messageInfo);

    void recallMessage(int id);

    PageResult<List<Message>> pageQuery(MessageQueryDTO messageQueryDTO);

    void deleteMessage(Integer id);
}
