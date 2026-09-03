package cn.wyq.serverwebsocket.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RechargeOrder：业务实体对象，用于映射和传递持久化数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("recharge_order")
public class RechargeOrder {
    @TableId
    private String outTradeNo;
    private Integer userId;
    private String alipayTradeNo;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
}