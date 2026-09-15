package cn.wyq.serverwebsocket.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 支付宝扫码订单状态。
 *
 * <p>状态查询同时反映本地入账状态和支付宝侧是否已经识别扫码，
 * 前端据此在二维码页面与支付处理中页面之间切换。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlipayPaymentStatusVO {
    /** 系统订单号。 */
    private String outTradeNo;
    /** WAITING_SCAN、SCANNED、CONFIRMING、PAID 或 CLOSED。 */
    private String status;
    /** 本地订单是否已经完成入账。 */
    private boolean paid;
    /** 支付宝是否已经识别到扫码。 */
    private boolean scanned;
    /** 订单金额。 */
    private BigDecimal amount;
    /**
     * 订单完成入账后从数据库读取的最新余额。
     * 只有 status=PAID 时才返回，避免前端在事务提交前显示旧余额。
     */
    private BigDecimal balance;
}
