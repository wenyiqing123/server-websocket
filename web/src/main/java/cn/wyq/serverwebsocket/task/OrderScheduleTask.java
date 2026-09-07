package cn.wyq.serverwebsocket.task;

import cn.wyq.serverwebsocket.mapper.RechargeOrderMapper;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.utils.SystemConfigUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单相关的定时任务
 */
/**
 * OrderScheduleTask：定时任务组件，负责按计划执行后台作业。
 */
@Slf4j
@Component
public class OrderScheduleTask {

    @Autowired
    private RechargeOrderMapper orderMapper;

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 定时取消超时未支付的订单
     * cron = "0 * * * * ?" 表示每分钟的第 0 秒执行一次
     */
    @Scheduled(cron = "0/10 * * * * ?")
    public void cancelUnpaidOrders() {
        try {
            // 从平台设置读取超时时间，配置异常时保持默认 5 分钟。
            int timeoutMinutes;
            try {
                timeoutMinutes = SystemConfigUtils.intValue(sysConfigService.getConfigMap(),
                        "recharge_close_minutes", 5, 1, 24 * 60);
            } catch (RuntimeException e) {
                log.warn("读取充值订单过期配置失败，将使用默认 5 分钟", e);
                timeoutMinutes = 5;
            }

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
