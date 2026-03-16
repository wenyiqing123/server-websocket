package cn.wyq.serverwebsocket.service;


import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.pojo.dto.MessageExportDTO;
import cn.wyq.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.pojo.socket.MessageFull;
import cn.wyq.serverwebsocket.pojo.socket.MessageInfo;

import java.util.List;

public interface MessageService {
    Integer addMessage(MessageInfo messageInfo);

    List<MessageFull> findAll(MessageInfo messageInfo);

    void recallMessage(int id);

    PageResult<List<Message>> pageQuery(MessageQueryDTO messageQueryDTO);

    void deleteMessage(Integer id);

    List<Message> export(MessageExportDTO messageExportDTO);
}
