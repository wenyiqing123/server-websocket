package cn.wyq.serverwebsocket.service;


import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.MessageExportDTO;
import cn.wyq.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.pojo.socket.MessageFull;
import cn.wyq.serverwebsocket.pojo.socket.MessageInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface MessageService  extends IService<Message> {
    Integer addMessage(MessageInfo messageInfo);

    List<MessageFull> findAll(MessageInfo messageInfo);

    void recallMessage(int id);
    Result clearHistory(String userName, String targetName);
    PageResult<List<Message>> pageQuery(MessageQueryDTO messageQueryDTO);

    void deleteMessage(Integer id);

    /**
     * 高级筛选查询聊天记录
     * @param userName 当前用户
     * @param targetName 聊天目标（好友名 或 群号）
     * @param type 筛选类型：0-全部，1-纯文本，2-图片，3-文件
     * @param keyword 搜索关键词
     * @return 过滤后的消息列表
     */
    Result getFilteredHistory(String userName, String targetName, Integer type, String keyword);
    List<Message> export(MessageExportDTO messageExportDTO);

    void deleteMessageByUser(Integer id);
}
