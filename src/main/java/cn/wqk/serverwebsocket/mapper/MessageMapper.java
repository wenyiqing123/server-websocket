package cn.wqk.serverwebsocket.mapper;

import cn.wqk.serverwebsocket.socket.pojo.MessageInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {
    Integer addMessage(MessageInfo messageInfo);
    List<MessageInfo> findAll(MessageInfo messageInfo);
}
