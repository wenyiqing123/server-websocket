package cn.wqk.serverwebsocket.service;

import cn.wqk.serverwebsocket.framework.common.PageResult;
import cn.wqk.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wqk.serverwebsocket.socket.pojo.MessageFull;
import cn.wqk.serverwebsocket.socket.pojo.MessageInfo;

import java.util.List;

public interface MessageService {
    Integer addMessage(MessageInfo messageInfo);

    List<MessageFull> findAll(MessageInfo messageInfo);

    void recallMessage(int id);

    PageResult pageQuery(MessageQueryDTO messageQueryDTO);

    void deleteMessage(Integer id);
}
