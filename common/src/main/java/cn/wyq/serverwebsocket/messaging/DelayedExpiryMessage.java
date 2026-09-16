package cn.wyq.serverwebsocket.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 资金到期检查消息。
 *
 * <p>step 表示当前时间块下标，delaySchedule 保存创建消息时确定的完整时间块，
 * 消费者可以据此安全地继续投递下一块，即使运行期间配置发生变化也不会改变原订单的
 * 到期节奏。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayedExpiryMessage {
    /** 业务类型，取 {@link ExpiryMessagingConstants} 中的常量。 */
    private String businessType;
    /** 业务主键，充值为 outTradeNo，红包/转账为数据库 ID。 */
    private String businessId;
    /** 当前时间块下标，从 0 开始。 */
    private Integer step;
    /** 创建时确定的连续延迟时间块，单位为秒。 */
    private List<Integer> delaySchedule;
    /** 当前消息在延迟队列中等待的秒数。 */
    private Integer delaySeconds;
}
