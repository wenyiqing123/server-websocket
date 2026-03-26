package cn.wyq.serverwebsocket.tool;

import cn.wyq.serverwebsocket.pojo.dto.MessageToolQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.service.IMessageServiceTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageTool {

    private final IMessageServiceTool messageServiceTool;

    // 💡 进化 1：极其详尽的描述说明。明确告诉 AI 这个工具的作用，以及它的返回限制。
    @Tool(description = "精确检索聊天消息记录。当被问到‘某某发了什么’、‘最近有哪些聊天记录’、‘帮我查一下关于xx的对话’时调用。注意：为防止数据过载，此接口最多只返回最新的20条匹配消息。")
    public List<Message> queryMessage(
            @ToolParam(description = "消息查询条件对象，可包含关键词(message)、发送人(fromName)、接收人(toName)以及时间范围等", required = false)
            MessageToolQueryDTO queryDTO){

        // 💡 进化 2：核弹级防爆盾。防空指针时，绝对不能全表查询！默认只给大模型返回全站最新的 20 条消息
        if (queryDTO == null) {
            return messageServiceTool.lambdaQuery()
                    // 💡 进化 3：过滤网。只挑选对 AI 有用的字段，如果你的 Message 表里有诸如 is_deleted 等冗余字段，这里就可以拦截掉
                    .select(Message::getId, Message::getMessage, Message::getFromName, Message::getToName, Message::getSendAt)
                    .orderByDesc(Message::getSendAt)
                    .last("LIMIT 20") // 强制安全底线
                    .list();
        }

        // 极致优雅的链式动态查询
        return messageServiceTool.lambdaQuery()
                // 过滤网：同样只查询核心字段给大模型
                .select(Message::getId, Message::getMessage, Message::getFromName, Message::getToName, Message::getSendAt)

                // ID查询
                .eq(queryDTO.getId() != null, Message::getId, queryDTO.getId())

                // 消息内容模糊查询
                .like(StringUtils.hasText(queryDTO.getMessage()), Message::getMessage, queryDTO.getMessage())

                // 发送人模糊查询
                .like(StringUtils.hasText(queryDTO.getFromName()), Message::getFromName, queryDTO.getFromName())

                // 接收人模糊查询
                .like(StringUtils.hasText(queryDTO.getToName()), Message::getToName, queryDTO.getToName())

                // 时间区间查询：大于等于开始时间 (Greater or Equal)
                .ge(queryDTO.getSendAtStart() != null, Message::getSendAt, queryDTO.getSendAtStart())

                // 时间区间查询：小于等于结束时间 (Less or Equal)
                .le(queryDTO.getSendAtEnd() != null, Message::getSendAt, queryDTO.getSendAtEnd())

                // 默认排序：按时间降序（最新发的消息排在最前面）
                .orderByDesc(Message::getSendAt)

                // 💡 进化 4：防爆盾。即使带了查询条件，也必须强制加上 LIMIT 20
                .last("LIMIT 20")

                // 执行查询
                .list();
    }
}