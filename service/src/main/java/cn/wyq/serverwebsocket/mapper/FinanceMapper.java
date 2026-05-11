package cn.wyq.serverwebsocket.mapper;


import cn.wyq.serverwebsocket.pojo.vo.BreakdownVO;
import cn.wyq.serverwebsocket.pojo.vo.FinanceSummaryVO;
import cn.wyq.serverwebsocket.pojo.vo.TrendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 财务数据统计 Mapper
 */
@Mapper
public interface FinanceMapper {

    /**
     * 获取核心财务数据汇总
     */
    FinanceSummaryVO getFinanceSummary(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取每日入账趋势
     */
    List<TrendVO> getRevenueTrend(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取资金消耗占比
     */
    List<BreakdownVO> getRevenueBreakdown(@Param("startDate") String startDate, @Param("endDate") String endDate);
}