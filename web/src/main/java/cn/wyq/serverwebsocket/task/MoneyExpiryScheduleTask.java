package cn.wyq.serverwebsocket.task;

import cn.wyq.serverwebsocket.mapper.RedPacketMapper;
import cn.wyq.serverwebsocket.mapper.TransferMapper;
import cn.wyq.serverwebsocket.service.RedPacketService;
import cn.wyq.serverwebsocket.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MoneyExpiryScheduleTask {

    private final RedPacketMapper redPacketMapper;
    private final RedPacketService redPacketService;
    private final TransferMapper transferMapper;
    private final TransferService transferService;

    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void refundExpiredMoney() {
        refundRedPackets();
        refundTransfers();
    }

    private void refundRedPackets() {
        try {
            for (Long redPacketId : redPacketMapper.selectExpiredRedPacketIds()) {
                try {
                    redPacketService.refundExpiredRedPacket(redPacketId);
                } catch (Exception e) {
                    log.error("红包过期退款失败，红包ID: {}", redPacketId, e);
                }
            }
        } catch (Exception e) {
            log.error("扫描过期红包失败", e);
        }
    }

    private void refundTransfers() {
        try {
            for (Long transferId : transferMapper.selectExpiredTransferIds()) {
                try {
                    transferService.refundExpiredTransfer(transferId);
                } catch (Exception e) {
                    log.error("转账过期退款失败，转账ID: {}", transferId, e);
                }
            }
        } catch (Exception e) {
            log.error("扫描过期转账失败，请确认已执行 u_transfer.sql", e);
        }
    }
}
