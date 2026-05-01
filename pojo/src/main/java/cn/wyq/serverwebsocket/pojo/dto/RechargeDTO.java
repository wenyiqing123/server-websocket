package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RechargeDTO {
    /**
     * 充值金额
     */
    private BigDecimal amount;
}