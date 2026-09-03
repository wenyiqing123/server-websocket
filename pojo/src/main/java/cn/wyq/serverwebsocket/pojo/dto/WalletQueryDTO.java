package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

/**
 * WalletQueryDTO：数据传输对象，用于承载接口请求参数。
 */
@Data
public class WalletQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Integer userId;
    private String userName;
}