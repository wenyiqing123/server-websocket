package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AccountLogVO：视图对象，用于组织接口返回和展示数据。
 */
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