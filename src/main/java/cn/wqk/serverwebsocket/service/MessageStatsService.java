package cn.wqk.serverwebsocket.service;

import cn.wqk.serverwebsocket.pojo.vo.ActiveUsersAndMessageCount;
import cn.wqk.serverwebsocket.pojo.vo.RencetSevenDayUsersAndMessageCount;
import cn.wqk.serverwebsocket.pojo.vo.UsersAndMessages;

import java.time.LocalDate;
import java.util.List;

public interface MessageStatsService {
    List<ActiveUsersAndMessageCount> getTodayActiveUsersAndMessageCount(LocalDate sendAtStart, LocalDate sendAtEnd);

    List<RencetSevenDayUsersAndMessageCount> getRencetSevenDayUsersAndMessageCount();

    List<UsersAndMessages> getActiveUsersAndMessages();
}
