package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.socket.pojo.MessageFull;
import cn.wyq.serverwebsocket.socket.pojo.MessageInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring") // 交给 Spring 管理
public interface MessageStructMapper {

    /**
     * 单个转换：将 MessageInfo 转为 MessageFull
     * ignore = true 表示这些字段我们将通过自定义逻辑手动设置
     */
    @Mapping(target = "fromPath", ignore = true)
    @Mapping(target = "toName", ignore = true)
    MessageFull toFull(MessageInfo info);

    /**
     * 集合转换：MapStruct 会自动循环调用上面的 toFull 方法
     */
    List<MessageFull> toFullList(List<MessageInfo> infoList);
}