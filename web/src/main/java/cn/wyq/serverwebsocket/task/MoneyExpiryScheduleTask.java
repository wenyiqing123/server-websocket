package cn.wyq.serverwebsocket.task;

import cn.wyq.serverwebsocket.mapper.RedPacketMapper;
import cn.wyq.serverwebsocket.mapper.TransferMapper;
import cn.wyq.serverwebsocket.service.RedPacketService;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.service.TransferService;
import cn.wyq.serverwebsocket.utils.SystemConfigUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * MoneyExpiryScheduleTask：定时任务组件，负责按计划执行后台作业。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoneyExpiryScheduleTask {

    private final RedPacketMapper redPacketMapper;
    private final RedPacketService redPacketService;
    private final TransferMapper transferMapper;
    private final TransferService transferService;
    private final SysConfigService sysConfigService;

    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void refundExpiredMoney() {
        int redPacketExpireHours = configuredHours("red_packet_expire_hours");
        int transferExpireHours = configuredHours("transfer_expire_hours");
        refundRedPackets(redPacketExpireHours);
        refundTransfers(transferExpireHours);
    }

    private void refundRedPackets(int expireHours) {
        try {
            for (Long redPacketId : redPacketMapper.selectExpiredRedPacketIds(expireHours)) {
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

    private void refundTransfers(int expireHours) {
        try {
            for (Long transferId : transferMapper.selectExpiredTransferIds(expireHours)) {
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

    /** 读取过期窗口，避免管理员误填导致定时任务失效。 */
    private int configuredHours(String key) {
        try {
            return SystemConfigUtils.intValue(sysConfigService.getConfigMap(), key,
                    24, 1, 24 * 365);
        } catch (RuntimeException e) {
            log.warn("读取资金过期配置失败，key={}，将使用默认 24 小时", key, e);
            return 24;
        }
    }
}
