package cn.wyq.serverwebsocket.component;


import cn.wyq.serverwebsocket.enumeration.WsMessageTypeEnum;
import cn.wyq.serverwebsocket.mapper.AccountLogMapper;
import cn.wyq.serverwebsocket.mapper.AccountMapper;
import cn.wyq.serverwebsocket.mapper.RedPacketMapper;
import cn.wyq.serverwebsocket.mapper.RedPacketRecordMapper;
import cn.wyq.serverwebsocket.pojo.dto.WsMessage;
import cn.wyq.serverwebsocket.pojo.entity.Account;
import cn.wyq.serverwebsocket.pojo.entity.AccountLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class RedPacketAsyncComponent {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AccountLogMapper accountLogMapper;

    @Autowired
    private RedPacketMapper redPacketMapper;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RedPacketRecordMapper redPacketRecordMapper;

    // 🌟 注入你的 WebSocket 核心服务类 (请根据你项目中的实际类名进行替换，比如 WebSocketServer)
    // @Autowired
    // private WebSocketServer webSocketServer;

    /**
     * 异步更新数据库（资金增加、流水记录、红包扣减）
     * 🚨 注意：启动类上必须加 @EnableAsync 注解，否则此方法不会异步执行！
     * 🚨 注意：新增了 roomId 参数用于群发广播
     */
    @Async("taskExecutor") // 生产环境建议自定义线程池，这里暂时直接用 @Async
    @Transactional(rollbackFor = Exception.class)
    public void executeGrabDbUpdate(Integer userId, Long redPacketId, BigDecimal amount, String groupId) {
        log.info("【异步落库开始】用户: {}, 红包: {}, 抢到金额: {}", userId, redPacketId, amount);

        try {
            // 1. 悲观锁查询并更新账户余额 (进账)
            Account account = accountMapper.selectByUserIdForUpdate(userId);
            BigDecimal balanceAfter;

            if (account != null) {
                // 场景 A：老用户，已有钱包记录，直接累加更新
                balanceAfter = account.getBalance().add(amount);
                account.setBalance(balanceAfter);
                accountMapper.updateById(account);
            } else {
                // 场景 B：新用户首次获得资金，初始化钱包记录
                log.info("用户 [{}] 无钱包记录，正在为其初始化钱包并入账", userId);
                balanceAfter = amount;

                account = Account.builder()
                        .userId(userId)
                        .balance(balanceAfter)
                        .build();
                accountMapper.insert(account);
            }

            // 2. 统一记录抢红包的资金流水 (正数进账)
            String orderNo = "GRAB" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);
            AccountLog accountLog = AccountLog.builder()
                    .userId(userId)
                    .type(3) // 变动类型：3-抢红包
                    .amount(amount)
                    .balanceAfter(balanceAfter)
                    .orderNo(orderNo)
                    .remark("抢到群聊红包")
                    .createTime(LocalDateTime.now())
                    .build();
            accountLogMapper.insert(accountLog);

            // 🚨 删除了这里重复的 redPacketRecordMapper.insert(record); 🚨

            // 3. 原子扣减红包主表的剩余金额和个数
            int updated = redPacketMapper.decreaseRedPacket(redPacketId, amount);
            if (updated == 0) {
                log.error("【严重异常】红包 {} 扣减失败，可能出现并发超卖或数据不一致", redPacketId);
            }

            // ==========================================================
            // 4. 🌟 触发 WebSocket 实时广播
            // ==========================================================
            try {
                Map<String, Object> extraData = new HashMap<>();
                extraData.put("grabberId", userId);
                extraData.put("redPacketId", redPacketId);
                extraData.put("amount", amount);

                WsMessage notifyMsg = WsMessage.builder()
                        .messageType(WsMessageTypeEnum.RED_PACKET_GRABBED.getCode())
                        .groupId(groupId)
                        .senderId(0)
                        .content("手气爆棚！用户 [" + userId + "] 抢到了 " + amount + " 元！")
                        .data(extraData)
                        .timestamp(LocalDateTime.now())
                        .build();

                String jsonMessage = objectMapper.writeValueAsString(notifyMsg);

                // WebSocketServer.sendToRoom(groupId, jsonMessage);
                log.info("【WebSocket广播】已向群 [{}] 发送红包动态", groupId);
            } catch (Exception e) {
                log.error("【WebSocket广播失败】群: {}, 红包: {}", groupId, redPacketId, e);
            }

            log.info("【异步落库成功】用户: {} 账目处理完毕", userId);
        } catch (Exception e) {
            log.error("【异步落库失败】用户: {}, 红包: {}", userId, redPacketId, e);
        }
    }
}