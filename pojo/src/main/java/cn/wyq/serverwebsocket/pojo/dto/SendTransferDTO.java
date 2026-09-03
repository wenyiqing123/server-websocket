package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SendTransferDTO {
    private String receiverName;
    private BigDecimal amount;
    private String remark;
}
