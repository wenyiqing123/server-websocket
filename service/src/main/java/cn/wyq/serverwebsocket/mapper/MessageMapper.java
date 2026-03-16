package cn.wyq.serverwebsocket.mapper;


import cn.wyq.serverwebsocket.pojo.dto.MessageExportDTO;
import cn.wyq.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.pojo.socket.MessageInfo;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MessageMapper {
    Integer addMessage(MessageInfo messageInfo);

    List<MessageInfo> findAll(MessageInfo messageInfo);

    @Update("update message set message = null where id = #{id}")
    void recallMessage(int id);

    @Select("SELECT *\n" +
            "FROM message\n" +
            "WHERE TIMESTAMPDIFF(MINUTE, send_at, NOW()) > 2 and id=#{id};")
    MessageInfo checkTime(int id);

    Page<Message> pageQuery(MessageQueryDTO messageQueryDTO);

    @Delete("delete  from message where id=#{id}")
    void deleteMessage(Integer id);

    List<Message> export(MessageExportDTO messageExportDTO);
}
