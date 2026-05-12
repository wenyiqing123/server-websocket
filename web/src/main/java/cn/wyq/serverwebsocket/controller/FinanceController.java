package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.vo.*;
import cn.wyq.serverwebsocket.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/finance")
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    // 1. 获取核心汇总数据
    @GetMapping("/summary")
    public Result getSummary(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        FinanceSummaryVO summary = financeService.getFinanceSummary(startDate, endDate);
        return Result.success(summary); // 假设你使用的是 Result 统一返回包装类
    }

    // 2. 获取每日流水折线趋势
    @GetMapping("/trend")
    public Result getTrend(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        List<TrendVO> trendList = financeService.getRevenueTrend(startDate, endDate);
        return Result.success(trendList);
    }

    // 3. 获取资金去向占比饼图
    @GetMapping("/breakdown")
    public Result getBreakdown(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        List<BreakdownVO> breakdownList = financeService.getRevenueBreakdown(startDate, endDate);
        return Result.success(breakdownList);
    }
    /**
     * 获取充值订单健康度
     */
    @GetMapping("/orderHealth")
    public Result getOrderHealth(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        List<OrderHealthVO> list = financeService.getOrderHealth(startDate, endDate);
        return Result.success(list);
    }

    /**
     * 获取红包生态流转对比
     */
    @GetMapping("/redPacketFlow")
    public Result getRedPacketFlow(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        List<RedPacketFlowVO> list = financeService.getRedPacketFlow(startDate, endDate);
        return Result.success(list);
    }

    /**
     * 获取全站财富风云榜 (前10名)
     */
    @GetMapping("/wealthRank")
    public Result getWealthRank() {
        List<WealthRankVO> list = financeService.getWealthRank();
        return Result.success(list);
    }
}