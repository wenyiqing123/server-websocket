package cn.wyq.serverwebsocket.tool;

import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.service.IMessageServiceTool;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageAnalysisTool {

    private final IMessageServiceTool messageServiceTool;

    @Tool(description = "获取绝对意义上最新的一条消息详情。当被问到‘最近一条消息是谁发的’、‘某某最后一次发消息是什么’时精确调用。")
    public Map<String, Object> getLatestSingleMessage(
            @ToolParam(description = "指定的发送人。如果问全服最新消息，此项留空", required = false) String fromName,
            @ToolParam(description = "指定的接收人。此项通常为空", required = false) String toName) {

        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.select("message", "from_name as fromName", "to_name as toName", "send_at as sendAt")
                .eq(StringUtils.hasText(fromName), "from_name", fromName)
                .eq(StringUtils.hasText(toName), "to_name", toName)
                // 💡 架构升级：内容不为 null 且不为空字符串，才算有效消息（过滤掉撤回消息）
                .isNotNull("message").ne("message", "")
                .orderByDesc("send_at")
                .last("LIMIT 1");

        List<Map<String, Object>> list = messageServiceTool.listMaps(wrapper);
        log.info("list is: {}", list);
        return list.isEmpty() ? null : list.get(0);
    }

    @Tool(description = "统计消息的具体数量。当被问到‘发了多少条消息’、‘一共聊了多少句’时调用。")
    public long countMessages(
            @ToolParam(description = "发送人姓名，可为空", required = false) String fromName,
            @ToolParam(description = "接收人姓名，可为空", required = false) String toName,
            @ToolParam(description = "计算最近多少天的数据。如‘最近三天’传3。不限时间传0", required = false) Integer days) {

        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.eq(StringUtils.hasText(fromName), "from_name", fromName)
                .eq(StringUtils.hasText(toName), "to_name", toName)
                // 💡 架构升级：统计时，绝不把撤回的消息（内容为空）算进去
                .isNotNull("message").ne("message", "");

        if (days != null && days > 0) {
            wrapper.apply("send_at >= DATE_SUB(NOW(), INTERVAL {0} DAY)", days);
        }

        return messageServiceTool.count(wrapper);
    }

    @Tool(description = "分析某人最常联系的人排行榜。当被问到‘他发给谁的消息最多’、‘他最常和谁聊天’时调用。")
    public List<Map<String, Object>> getTopContacts(
            @ToolParam(description = "必须指定发送人姓名", required = true) String fromName,
            @ToolParam(description = "最近几天的数据。如‘最近三天’传3。不限时间传0", required = false) Integer days) {

        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.select("to_name as contactPerson", "COUNT(*) as messageCount")
                .eq("from_name", fromName)
                // 💡 架构升级：排除群发消息（接收人为空），群发不能算作“常联系的人”
                .isNotNull("to_name").ne("to_name", "")
                // 💡 架构升级：排除撤回消息（内容为空）
                .isNotNull("message").ne("message", "");

        if (days != null && days > 0) {
            wrapper.apply("send_at >= DATE_SUB(NOW(), INTERVAL {0} DAY)", days);
        }

        wrapper.groupBy("to_name")
                .orderByDesc("messageCount")
                .last("LIMIT 5");

        return messageServiceTool.listMaps(wrapper);
    }
}