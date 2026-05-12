package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WealthRankVO {
    private String userName;   // 用户名
    private BigDecimal balance; // 账户余额
}