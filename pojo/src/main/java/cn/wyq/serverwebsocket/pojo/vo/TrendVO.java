package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * TrendVO：视图对象，用于组织接口返回和展示数据。
 */
@Data
public class TrendVO {
    private String date;      // 日期 (格式: YYYY-MM-DD)
    private BigDecimal amount; // 当日金额
}