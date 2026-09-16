package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.LocalMessageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 本地消息表数据访问接口。 */
@Mapper
public interface LocalMessageLogMapper {

    /** 插入一条待投递消息。 */
    int insertPending(LocalMessageLog message);

    /** 按业务消息 ID 幂等插入一条待投递消息。 */
    int insertPendingIdempotent(LocalMessageLog message);

    /** 查询当前需要投递的消息。 */
    List<LocalMessageLog> selectDispatchable(@Param("limit") int limit);

    /** 抢占一条消息，防止多实例重复发送同一条记录。 */
    int claim(@Param("id") Long id);

    /** 将超时未完成的发送任务重新放回重试队列。 */
    int resetStaleSending(@Param("staleMinutes") int staleMinutes);

    /** 根据消息 ID 查询记录。 */
    LocalMessageLog selectByMessageId(@Param("messageId") String messageId);

    /** 收到 RabbitMQ 确认后标记成功。 */
    int markConfirmed(@Param("messageId") String messageId);

    /** 网络异常或 NACK 后安排重试。 */
    int markRetry(@Param("messageId") String messageId,
                  @Param("errorMessage") String errorMessage,
                  @Param("delaySeconds") int delaySeconds);

    /** 消息被 RabbitMQ 返回时安排重试并记录返回信息。 */
    int markReturned(@Param("messageId") String messageId,
                     @Param("errorMessage") String errorMessage,
                     @Param("delaySeconds") int delaySeconds);
}
