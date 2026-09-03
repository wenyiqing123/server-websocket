package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * WealthRankVO：视图对象，用于组织接口返回和展示数据。
 */
@Data
public class WealthRankVO {
    private String userName;   // 用户名
    private BigDecimal balance; // 账户余额
}