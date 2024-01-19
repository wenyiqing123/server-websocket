package cn.wqk.serverwebsocket.service;

import cn.wqk.serverwebsocket.socket.pojo.MessageInfo;

import java.util.List;

public interface MessageService {
    Integer addMessage(MessageInfo messageInfo);
    List<MessageInfo> findAll(MessageInfo messageInfo);
}
