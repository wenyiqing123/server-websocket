package cn.wqk.serverwebsocket.mapper;

import cn.wqk.serverwebsocket.pojo.vo.ActiveUsersAndMessageCount;
import cn.wqk.serverwebsocket.pojo.vo.RencetSevenDayUsersAndMessageCount;
import cn.wqk.serverwebsocket.pojo.vo.UsersAndMessages;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MessageStatsMapper {
    List<ActiveUsersAndMessageCount> getTodayActiveUsersAndMessageCount(LocalDate sendAtStart, LocalDate sendAtEnd);

    List<RencetSevenDayUsersAndMessageCount> getRencetSevenDayUsersAndMessageCount(LocalDate minus, LocalDate now);

    List<UsersAndMessages> getActiveUsersAndMessages(LocalDate minus, LocalDate now);
}
