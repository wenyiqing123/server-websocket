package cn.wyq.serverwebsocket.service.impl;


import cn.wyq.serverwebsocket.mapper.MessageStatsMapper;
import cn.wyq.serverwebsocket.pojo.vo.ActiveUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.RencetSevenDayUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.UsersAndMessages;
import cn.wyq.serverwebsocket.service.MessageStatsService;
import cn.wyq.serverwebsocket.utils.CacheClient;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * MessageStatsImpl：业务服务实现类，封装对应领域的业务规则。
 */
@Service
public class MessageStatsImpl implements MessageStatsService {


    @Autowired
    private MessageStatsMapper messageStatsMapper;
    @Autowired
    private CacheClient cacheClient;

    @Override
    public List<ActiveUsersAndMessageCount> getTodayActiveUsersAndMessageCount(LocalDate sendAtStart, LocalDate sendAtEnd) {
        LocalDate[] range = normalizeRange(sendAtStart, sendAtEnd);
        return cacheClient.queryWithMutex("stats:today-active", rangeKey(range),
                new TypeReference<List<ActiveUsersAndMessageCount>>() {},
                ignored -> emptyIfNull(messageStatsMapper.getTodayActiveUsersAndMessageCount(range[0], range[1])),
                2, java.util.concurrent.TimeUnit.MINUTES);
    }

    @Override
    public List<RencetSevenDayUsersAndMessageCount> getRencetSevenDayUsersAndMessageCount(LocalDate sendAtStart,LocalDate sendAtEnd) {
        LocalDate[] range = normalizeRange(sendAtStart, sendAtEnd);
        return cacheClient.queryWithMutex("stats:recent-active", rangeKey(range),
                new TypeReference<List<RencetSevenDayUsersAndMessageCount>>() {},
                ignored -> emptyIfNull(messageStatsMapper.getRencetSevenDayUsersAndMessageCount(range[0], range[1])),
                2, java.util.concurrent.TimeUnit.MINUTES);
    }

    @Override
    public List<UsersAndMessages> getActiveUsersAndMessages(LocalDate sendAtStart,LocalDate sendAtEnd) {
        LocalDate[] range = normalizeRange(sendAtStart, sendAtEnd);
        return cacheClient.queryWithMutex("stats:users-messages", rangeKey(range),
                new TypeReference<List<UsersAndMessages>>() {},
                ignored -> emptyIfNull(messageStatsMapper.getActiveUsersAndMessages(range[0], range[1])),
                2, java.util.concurrent.TimeUnit.MINUTES);
    }

    private LocalDate[] normalizeRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("统计时间范围不能为空");
        }
        if (start.equals(end)) {
            return new LocalDate[]{start, end.plusDays(1)};
        }
        return new LocalDate[]{start.plusDays(1), end.plusDays(1)};
    }

    private String rangeKey(LocalDate[] range) {
        return range[0] + "|" + range[1];
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? List.of() : values;
    }
}
