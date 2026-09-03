package cn.wyq.serverwebsocket.service;


import cn.wyq.serverwebsocket.pojo.vo.ActiveUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.RencetSevenDayUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.UsersAndMessages;

import java.time.LocalDate;
import java.util.List;

/**
 * MessageStatsService：业务服务接口，定义对应领域的业务操作契约。
 */
public interface MessageStatsService {
    List<ActiveUsersAndMessageCount> getTodayActiveUsersAndMessageCount(LocalDate sendAtStart, LocalDate sendAtEnd);

    List<RencetSevenDayUsersAndMessageCount> getRencetSevenDayUsersAndMessageCount(LocalDate sendAtStart,LocalDate sendAtEnd);

    List<UsersAndMessages> getActiveUsersAndMessages(LocalDate sendAtStart,LocalDate sendAtEnd);
}
