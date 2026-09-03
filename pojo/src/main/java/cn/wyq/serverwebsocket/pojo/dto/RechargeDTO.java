package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * RechargeDTO：数据传输对象，用于承载接口请求参数。
 */
@Data
public class RechargeDTO {
    /**
     * 充值金额
     */
    private BigDecimal amount;
}