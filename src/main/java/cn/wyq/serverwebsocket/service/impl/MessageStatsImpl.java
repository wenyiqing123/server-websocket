package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.mapper.MessageStatsMapper;
import cn.wyq.serverwebsocket.pojo.vo.ActiveUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.RencetSevenDayUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.UsersAndMessages;
import cn.wyq.serverwebsocket.service.MessageStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class MessageStatsImpl implements MessageStatsService {


    @Autowired
    private MessageStatsMapper messageStatsMapper;

    @Override
    public List<ActiveUsersAndMessageCount> getTodayActiveUsersAndMessageCount(LocalDate sendAtStart, LocalDate sendAtEnd) {
        if (sendAtStart.equals(sendAtEnd)) {
            sendAtEnd = sendAtEnd.plusDays(1);
        }
        List<ActiveUsersAndMessageCount> activeUsersAndMessageCountList = messageStatsMapper.getTodayActiveUsersAndMessageCount(sendAtStart, sendAtEnd);
        return activeUsersAndMessageCountList;
    }

    @Override
    public List<RencetSevenDayUsersAndMessageCount> getRencetSevenDayUsersAndMessageCount() {
        LocalDate now = LocalDate.now();
        LocalDate minus = now.minus(60, ChronoUnit.DAYS);
        List<RencetSevenDayUsersAndMessageCount> rencetSevenDayUsersAndMessageCountList = messageStatsMapper.getRencetSevenDayUsersAndMessageCount(minus, now);
        return rencetSevenDayUsersAndMessageCountList;
    }

    @Override
    public List<UsersAndMessages> getActiveUsersAndMessages() {
        LocalDate now = LocalDate.now();
        LocalDate minus = now.minus(60, ChronoUnit.DAYS);
        List<UsersAndMessages> usersAndMessagesList = messageStatsMapper.getActiveUsersAndMessages(minus, now);
        return usersAndMessagesList;
    }
}