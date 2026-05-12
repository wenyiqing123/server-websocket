package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.mapper.FinanceMapper;
import cn.wyq.serverwebsocket.pojo.vo.*;
import cn.wyq.serverwebsocket.service.FinanceService;
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

    @Override
    public FinanceSummaryVO getFinanceSummary(String startDate, String endDate) {
        // 直接调用 Mapper，如果查询结果为空，MyBatis 会返回 null，可以做个兜底处理
        FinanceSummaryVO summary = financeMapper.getFinanceSummary(startDate, endDate);
        if (summary == null) {
            return new FinanceSummaryVO(); // 返回默认对象，防止前端报 NullPointerException
        }
        return summary;
    }

    @Override
    public List<TrendVO> getRevenueTrend(String startDate, String endDate) {
        return financeMapper.getRevenueTrend(startDate, endDate);
    }

    @Override
    public List<BreakdownVO> getRevenueBreakdown(String startDate, String endDate) {
        return financeMapper.getRevenueBreakdown(startDate, endDate);
    }
    @Override
    public List<OrderHealthVO> getOrderHealth(String startDate, String endDate) {
        return financeMapper.getOrderHealth(startDate, endDate);
    }

    @Override
    public List<RedPacketFlowVO> getRedPacketFlow(String startDate, String endDate) {
        return financeMapper.getRedPacketFlow(startDate, endDate);
    }

    @Override
    public List<WealthRankVO> getWealthRank() {
        return financeMapper.getWealthRank();
    }
}