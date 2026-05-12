package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserWalletVO {
    private Integer userId;
    private String userName;
    private BigDecimal balance;
    private BigDecimal freezeAmount;
    private String updateTime;
}
