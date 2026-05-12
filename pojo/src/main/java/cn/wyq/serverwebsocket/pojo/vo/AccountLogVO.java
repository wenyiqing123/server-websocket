package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountLogVO {
    private Long id;
    private Integer userId;
    private String userName;
    private Integer type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String orderNo;
    private String remark;
    private String createTime;
}