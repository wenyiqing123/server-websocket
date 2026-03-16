package cn.wyq.serverwebsocket.service.impl;


import cn.wyq.serverwebsocket.mapper.MessageStatsMapper;
import cn.wyq.serverwebsocket.pojo.vo.ActiveUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.RencetSevenDayUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.UsersAndMessages;
import cn.wyq.serverwebsocket.service.MessageStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MessageStatsImpl implements MessageStatsService {


    @Autowired
    private MessageStatsMapper messageStatsMapper;

    @Override
    public List<ActiveUsersAndMessageCount> getTodayActiveUsersAndMessageCount(LocalDate sendAtStart, LocalDate sendAtEnd) {
        if (sendAtStart.equals(sendAtEnd)) {
            sendAtEnd = sendAtEnd.plusDays(1);
        }else {
            sendAtStart = sendAtStart.plusDays(1);
            sendAtEnd = sendAtEnd.plusDays(1);
        }
        List<ActiveUsersAndMessageCount> activeUsersAndMessageCountList = messageStatsMapper.getTodayActiveUsersAndMessageCount(sendAtStart, sendAtEnd);
        return activeUsersAndMessageCountList;
    }

    @Override
    public List<RencetSevenDayUsersAndMessageCount> getRencetSevenDayUsersAndMessageCount(LocalDate sendAtStart,LocalDate sendAtEnd) {
        if (sendAtStart.equals(sendAtEnd)) {
            sendAtEnd = sendAtEnd.plusDays(1);
        }else {
            sendAtStart = sendAtStart.plusDays(1);
            sendAtEnd = sendAtEnd.plusDays(1);
        }
        List<RencetSevenDayUsersAndMessageCount> rencetSevenDayUsersAndMessageCountList = messageStatsMapper.getRencetSevenDayUsersAndMessageCount(sendAtStart, sendAtEnd);
        return rencetSevenDayUsersAndMessageCountList;
    }

    @Override
    public List<UsersAndMessages> getActiveUsersAndMessages(LocalDate sendAtStart,LocalDate sendAtEnd) {
        if (sendAtStart.equals(sendAtEnd)) {
            sendAtEnd = sendAtEnd.plusDays(1);
        }else {
            sendAtStart = sendAtStart.plusDays(1);
            sendAtEnd = sendAtEnd.plusDays(1);
        }
        List<UsersAndMessages> usersAndMessagesList = messageStatsMapper.getActiveUsersAndMessages(sendAtStart, sendAtEnd);
        return usersAndMessagesList;
    }
}