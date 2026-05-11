package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinanceSummaryVO {
    private BigDecimal totalRevenue;   // 期间总入账
    private BigDecimal todayRevenue;   // 今日新增充值
    private BigDecimal redPacketTotal; // 红包流水
    private BigDecimal aiCostTotal;    // AI算力消耗
}