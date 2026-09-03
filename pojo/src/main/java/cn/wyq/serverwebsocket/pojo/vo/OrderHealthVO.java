package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

/**
 * OrderHealthVO：视图对象，用于组织接口返回和展示数据。
 */
@Data
public class OrderHealthVO {
    private Integer status; // 订单状态: 0-待支付, 1-成功, 2-关闭
    private Integer count;  // 数量
}
