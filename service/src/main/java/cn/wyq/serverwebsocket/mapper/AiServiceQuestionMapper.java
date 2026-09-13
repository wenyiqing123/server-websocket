package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.AiServiceQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 智能客服问题题库数据访问接口。
 */
@Mapper
public interface AiServiceQuestionMapper {

    /**
     * 新增问题，或在问题已存在时增加使用次数。
     *
     * @return MySQL 受影响行数
     */
    int upsert(@Param("questionHash") String questionHash,
               @Param("question") String question,
               @Param("normalizedQuestion") String normalizedQuestion,
               @Param("lastUserId") Integer lastUserId);

    /**
     * 查询用于启动预热的客服问题。
     *
     * @param limit 最多加载条数
     * @return 题库记录
     */
    List<AiServiceQuestion> selectForWarmup(@Param("limit") int limit);

    /**
     * 读取旧版 JDBC 聊天记忆中的客服用户问题，作为新题库上线时的兼容数据源。
     *
     * @param limit 最多读取条数
     * @return 历史用户问题文本
     */
    List<String> selectHistoricalServiceQuestions(@Param("limit") int limit);

    /** 记录用户真正发送问题时，发送前是否命中已有提示问题。 */
    int insertQuestionLog(@Param("userId") Integer userId,
                          @Param("question") String question,
                          @Param("matchedQuestion") String matchedQuestion,
                          @Param("similarity") Double similarity,
                          @Param("matched") boolean matched);
}
