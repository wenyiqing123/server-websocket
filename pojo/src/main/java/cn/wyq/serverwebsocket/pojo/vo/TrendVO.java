package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TrendVO {
    private String date;      // 日期 (格式: YYYY-MM-DD)
    private BigDecimal amount; // 当日金额
}