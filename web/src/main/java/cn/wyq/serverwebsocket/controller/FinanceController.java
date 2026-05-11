package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.vo.BreakdownVO;
import cn.wyq.serverwebsocket.pojo.vo.FinanceSummaryVO;
import cn.wyq.serverwebsocket.pojo.vo.TrendVO;
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
}