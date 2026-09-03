package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * BreakdownVO：视图对象，用于组织接口返回和展示数据。
 */
@Data
public class BreakdownVO {
    private String type;       // 类型名称 (例如: 购买算力, 发送红包)
    private BigDecimal amount; // 该类型总金额
}