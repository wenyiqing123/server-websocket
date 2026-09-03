package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * SendTransferDTO：数据传输对象，用于承载接口请求参数。
 */
@Data
public class SendTransferDTO {
    private String receiverName;
    private BigDecimal amount;
    private String remark;
}
