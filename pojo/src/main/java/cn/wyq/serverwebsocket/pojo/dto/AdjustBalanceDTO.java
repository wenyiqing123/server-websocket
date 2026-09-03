package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AdjustBalanceDTO：数据传输对象，用于承载接口请求参数。
 */
@Data
public class AdjustBalanceDTO {
    private Integer userId;
    private Integer actionType; // 1: 加款 (增加余额), 2: 扣款 (减少余额)
    private BigDecimal amount;  // 必须为正数，正负号在业务层处理
    private String remark;      // 调账备注
}