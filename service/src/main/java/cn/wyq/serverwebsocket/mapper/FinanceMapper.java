package cn.wyq.serverwebsocket.mapper;


import cn.wyq.serverwebsocket.pojo.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 财务数据统计 Mapper
 */
/**
 * FinanceMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
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
    // 充值订单健康度
    List<OrderHealthVO> getOrderHealth(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 红包生态流转对比
    List<RedPacketFlowVO> getRedPacketFlow(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 全站财富风云榜 (排行榜与时间无关，查询当前实时状态)
    List<WealthRankVO> getWealthRank();
}