package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * UserWalletVO：视图对象，用于组织接口返回和展示数据。
 */
@Data
public class UserWalletVO {
    private Integer userId;
    private String userName;
    private BigDecimal balance;
    private BigDecimal freezeAmount;
    private String updateTime;
}
