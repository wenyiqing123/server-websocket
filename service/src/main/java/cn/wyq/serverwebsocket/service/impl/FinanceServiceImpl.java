package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.mapper.FinanceMapper;
import cn.wyq.serverwebsocket.pojo.vo.*;
import cn.wyq.serverwebsocket.service.FinanceService;
import cn.wyq.serverwebsocket.utils.CacheClient;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 财务数据统计 Service 实现类
 */
@Service
public class FinanceServiceImpl implements FinanceService {

    @Autowired
    private FinanceMapper financeMapper;
    @Autowired
    private CacheClient cacheClient;

    @Override
    public FinanceSummaryVO getFinanceSummary(String startDate, String endDate) {
        return cacheClient.queryWithMutex("finance:summary", dateRangeKey(startDate, endDate),
                FinanceSummaryVO.class, ignored -> {
                    FinanceSummaryVO summary = financeMapper.getFinanceSummary(startDate, endDate);
                    return summary == null ? new FinanceSummaryVO() : summary;
                }, 2, java.util.concurrent.TimeUnit.MINUTES);
    }

    @Override
    public List<TrendVO> getRevenueTrend(String startDate, String endDate) {
        return cacheClient.queryWithMutex("finance:revenue-trend", dateRangeKey(startDate, endDate),
                new TypeReference<List<TrendVO>>() {},
                ignored -> emptyIfNull(financeMapper.getRevenueTrend(startDate, endDate)),
                2, java.util.concurrent.TimeUnit.MINUTES);
    }

    @Override
    public List<BreakdownVO> getRevenueBreakdown(String startDate, String endDate) {
        return cacheClient.queryWithMutex("finance:revenue-breakdown", dateRangeKey(startDate, endDate),
                new TypeReference<List<BreakdownVO>>() {},
                ignored -> emptyIfNull(financeMapper.getRevenueBreakdown(startDate, endDate)),
                2, java.util.concurrent.TimeUnit.MINUTES);
    }
    @Override
    public List<OrderHealthVO> getOrderHealth(String startDate, String endDate) {
        return cacheClient.queryWithMutex("finance:order-health", dateRangeKey(startDate, endDate),
                new TypeReference<List<OrderHealthVO>>() {},
                ignored -> emptyIfNull(financeMapper.getOrderHealth(startDate, endDate)),
                2, java.util.concurrent.TimeUnit.MINUTES);
    }

    @Override
    public List<RedPacketFlowVO> getRedPacketFlow(String startDate, String endDate) {
        return cacheClient.queryWithMutex("finance:red-packet-flow", dateRangeKey(startDate, endDate),
                new TypeReference<List<RedPacketFlowVO>>() {},
                ignored -> emptyIfNull(financeMapper.getRedPacketFlow(startDate, endDate)),
                2, java.util.concurrent.TimeUnit.MINUTES);
    }

    @Override
    public List<WealthRankVO> getWealthRank() {
        return cacheClient.queryWithMutex("finance:wealth-rank", "all",
                new TypeReference<List<WealthRankVO>>() {},
                ignored -> emptyIfNull(financeMapper.getWealthRank()),
                2, java.util.concurrent.TimeUnit.MINUTES);
    }

    private String dateRangeKey(String startDate, String endDate) {
        return String.valueOf(startDate) + "|" + String.valueOf(endDate);
    }

    private <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? List.of() : list;
    }
}
