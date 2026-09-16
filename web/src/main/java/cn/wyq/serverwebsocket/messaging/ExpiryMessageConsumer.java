package cn.wyq.serverwebsocket.messaging;

import cn.wyq.serverwebsocket.service.DelayedExpiryMessageScheduler;
import cn.wyq.serverwebsocket.service.RedPacketService;
import cn.wyq.serverwebsocket.service.RechargeService;
import cn.wyq.serverwebsocket.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 资金到期检查消费者。
 *
 * <p>每次收到消息只做一次数据库检查。业务仍未完成时继续投递下一个时间块，
 * 最后一块才执行充值关单、红包退款或转账退款。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiryMessageConsumer {

    private final DelayedExpiryMessageScheduler scheduler;
    private final RechargeService rechargeService;
    private final RedPacketService redPacketService;
    private final TransferService transferService;

    @RabbitListener(queues = ExpiryMessagingConstants.CHECK_QUEUE)
    public void consume(DelayedExpiryMessage message) {
        scheduler.validate(message);
        switch (message.getBusinessType()) {
            case ExpiryMessagingConstants.BUSINESS_RECHARGE_ORDER ->
                    rechargeService.processExpiryCheck(message);
            case ExpiryMessagingConstants.BUSINESS_RED_PACKET ->
                    redPacketService.processExpiryCheck(message);
            case ExpiryMessagingConstants.BUSINESS_TRANSFER ->
                    transferService.processExpiryCheck(message);
            default -> throw new IllegalArgumentException("不支持的资金到期业务类型："
                    + message.getBusinessType());
        }
        log.debug("资金到期检查完成，businessType={}, businessId={}, step={}",
                message.getBusinessType(), message.getBusinessId(), message.getStep());
    }
}
