package cn.wyq.serverwebsocket.mapper;


import cn.wyq.serverwebsocket.pojo.vo.ActiveUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.RencetSevenDayUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.UsersAndMessages;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MessageStatsMapper {
    List<ActiveUsersAndMessageCount> getTodayActiveUsersAndMessageCount(LocalDate sendAtStart, LocalDate sendAtEnd);

    List<RencetSevenDayUsersAndMessageCount> getRencetSevenDayUsersAndMessageCount(LocalDate sendAtStart, LocalDate sendAtEnd);

    List<UsersAndMessages> getActiveUsersAndMessages(LocalDate sendAtStart, LocalDate sendAtEnd);
}
