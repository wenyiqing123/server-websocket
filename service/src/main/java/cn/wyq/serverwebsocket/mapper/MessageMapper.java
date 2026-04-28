package cn.wyq.serverwebsocket.mapper;


import cn.wyq.serverwebsocket.pojo.dto.MessageExportDTO;
import cn.wyq.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.pojo.socket.MessageInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    Integer addMessage(MessageInfo messageInfo);
    /**
     * 清空两人之间的所有聊天记录
     */
    int deleteChatHistory(@Param("user1") String user1, @Param("user2") String user2);
    List<MessageInfo> findAll(MessageInfo messageInfo);

    @Update("update message set message = null where id = #{id}")
    void recallMessage(int id);

    @Select("SELECT *\n" +
            "FROM message\n" +
            "WHERE TIMESTAMPDIFF(MINUTE, send_at, NOW()) < 2 and id=#{id};")
    MessageInfo checkTime(int id);

    Page<Message> pageQuery(MessageQueryDTO messageQueryDTO);

    @Delete("delete  from message where id=#{id}")
    void deleteMessage(Integer id);

    List<Message> export(MessageExportDTO messageExportDTO);

    void deleteMessageByUser(Integer id);

    @Delete("delete from message where to_name=#{group};")
    void deleteMessagesByTarget(String group);

    @Delete("delete from message where from_name=#{userName} or to_name=#{userName};")
    void deleteAllMessagesByUserName(String userName);

    // 新增：不分页的全量条件查询
    List<Message> selectListByCondition(MessageQueryDTO messageQueryDTO);
}
