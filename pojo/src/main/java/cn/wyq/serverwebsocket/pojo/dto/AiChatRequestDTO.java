package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 流式聊天请求。
 *
 * 聊天正文与会话标识必须放在 POST 请求体中，避免出现在 URL、浏览器历史和访问日志里。
 */
@Data
public class AiChatRequestDTO {

    /** 本轮用户输入。 */
    private String message;
    /** 已创建的会话编号。 */
    private String sessionId;
    /** girlfriend、service、document 或 custom。 */
    private String agentType;
    /** 自定义智能体编号，仅 custom 模式允许传入。 */
    private Long agentId;
    /** 兼容单文档调用的文档编号。 */
    private String docId;
    /** 文档分析会话本轮挂载的文档编号。 */
    private List<String> docIds;
}
