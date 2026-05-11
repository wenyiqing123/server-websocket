package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.pojo.vo.BreakdownVO;
import cn.wyq.serverwebsocket.pojo.vo.FinanceSummaryVO;
import cn.wyq.serverwebsocket.pojo.vo.TrendVO;

import java.util.List;

/**
 * 财务数据统计 Service 接口
 */
public interface FinanceService {

    /**
     * 获取核心财务数据汇总
     */
    FinanceSummaryVO getFinanceSummary(String startDate, String endDate);

    /**
     * 获取每日入账趋势
     */
    List<TrendVO> getRevenueTrend(String startDate, String endDate);

    /**
     * 获取资金消耗占比
     */
    List<BreakdownVO> getRevenueBreakdown(String startDate, String endDate);
}