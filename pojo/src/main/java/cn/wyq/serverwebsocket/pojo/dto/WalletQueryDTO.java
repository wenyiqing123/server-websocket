package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

@Data
public class WalletQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Integer userId;
    private String userName;
}