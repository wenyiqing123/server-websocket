package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

/**
 * OrderQueryDTO：视图对象，用于组织接口返回和展示数据。
 */
@Data
public class OrderQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String outTradeNo;
    private Integer userId;
    private Integer status;
}