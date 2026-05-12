package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

@Data
public class OrderQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String outTradeNo;
    private Integer userId;
    private Integer status;
}