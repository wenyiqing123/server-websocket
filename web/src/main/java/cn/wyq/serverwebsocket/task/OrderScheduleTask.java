package cn.wyq.serverwebsocket.task;

import cn.wyq.serverwebsocket.mapper.RechargeOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单相关的定时任务
 */
@Slf4j
@Component
public class OrderScheduleTask {

    @Autowired
    private RechargeOrderMapper orderMapper;

    /**
     * 定时取消超时未支付的订单
     * cron = "0 * * * * ?" 表示每分钟的第 0 秒执行一次
     */
    @Scheduled(cron = "0/10 * * * * ?")
    public void cancelUnpaidOrders() {
        try {
            // 🌟 设定超时时间为 5 分钟 (与前端和支付宝保持完全一致)
            int timeoutMinutes = 5;

            // 执行批量关单
            int closedCount = orderMapper.closeExpiredOrders(timeoutMinutes);

            // 如果真的扫到了并关闭了订单，就在控制台打印一条日志
            if (closedCount > 0) {
                log.info("【订单清理任务】成功扫描并自动关闭了 {} 笔超时未支付的充值订单。", closedCount);
            }
        } catch (Exception e) {
            log.error("【订单清理任务】执行异常: ", e);
        }
    }
}