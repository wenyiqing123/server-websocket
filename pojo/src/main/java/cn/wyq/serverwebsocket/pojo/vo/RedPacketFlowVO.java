package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RedPacketFlowVO {
    private String date;               // 日期 YYYY-MM-DD
    private BigDecimal sentAmount;     // 发出红包总额
    private BigDecimal receivedAmount; // 抢到红包总额
}
