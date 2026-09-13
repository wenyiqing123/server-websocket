package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.pojo.vo.AiServiceQuestionSuggestionVO;

import java.util.List;

/**
 * 智能客服问题持久化、预热和输入联想服务。
 */
public interface ServiceQuestionSuggestionService {

    /** 启动时从数据库加载题库到内存。 */
    void warmUp();

    /** 持久化一次用户发送的问题。 */
    void recordQuestion(Integer userId, String question);

    /** 根据当前输入返回相似度最高的问题。 */
    List<AiServiceQuestionSuggestionVO> suggest(String query, int limit);

    /** 清空并重新加载题库缓存，供管理员修改题库后即时生效。 */
    void refreshCache();
}
