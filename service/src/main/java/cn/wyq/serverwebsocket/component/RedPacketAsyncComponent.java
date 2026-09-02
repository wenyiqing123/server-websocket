package cn.wyq.serverwebsocket.component;


import cn.wyq.serverwebsocket.enumeration.WsMessageTypeEnum;
import cn.wyq.serverwebsocket.mapper.AccountLogMapper;
import cn.wyq.serverwebsocket.mapper.AccountMapper;
import cn.wyq.serverwebsocket.mapper.RedPacketMapper;
import cn.wyq.serverwebsocket.pojo.dto.WsMessage;
import cn.wyq.serverwebsocket.pojo.entity.Account;
import cn.wyq.serverwebsocket.pojo.entity.AccountLog;
import cn.wyq.serverwebsocket.pojo.entity.RedPacket;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * 在同一事务内完成红包校验、扣减、入账和流水记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeGrabDbUpdate(Integer userId, Long redPacketId, BigDecimal amount, String groupId) {
        log.info("【红包落库开始】用户: {}, 红包: {}, 抢到金额: {}", userId, redPacketId, amount);

        RedPacket redPacket = redPacketMapper.selectByIdForUpdate(redPacketId);
        if (redPacket == null || redPacket.getStatus() == null || redPacket.getStatus() != 0) {
            throw new IllegalStateException("红包已结束，无法领取");
        }
        if (redPacket.getCreateTime() == null
                || !redPacket.getCreateTime().isAfter(LocalDateTime.now().minusDays(1))) {
            throw new IllegalStateException("红包已过期，未领取金额将退回发送者");
        }
        if (redPacket.getRemainingAmount() == null
                || redPacket.getRemainingAmount().compareTo(amount) < 0
                || redPacket.getRemainingCount() == null
                || redPacket.getRemainingCount() <= 0) {
            throw new IllegalStateException("红包余额不足，请稍后重试");
        }

        int updated = redPacketMapper.decreaseRedPacket(redPacketId, amount);
        if (updated != 1) {
            throw new IllegalStateException("红包已被领取或已退回");
        }

        Account account = accountMapper.selectByUserIdForUpdate(userId);
        BigDecimal balanceAfter;
        if (account == null) {
            balanceAfter = amount;
            accountMapper.insert(Account.builder().userId(userId).balance(balanceAfter).build());
        } else {
            balanceAfter = account.getBalance().add(amount);
            account.setBalance(balanceAfter);
            accountMapper.updateById(account);
        }

        accountLogMapper.insert(AccountLog.builder()
                .userId(userId)
                .type(3)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .orderNo("GRAB" + redPacketId + "_" + UUID.randomUUID().toString().substring(0, 8))
                .remark("抢到红包")
                .createTime(LocalDateTime.now())
                .build());

        if (redPacket.getRemainingCount() == 1) {
            redPacketMapper.markRedPacketFinished(redPacketId);
        }

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

            objectMapper.writeValueAsString(notifyMsg);
            log.info("【WebSocket广播】已向群 [{}] 发送红包动态", groupId);
        } catch (Exception e) {
            log.error("【WebSocket广播失败】群: {}, 红包: {}", groupId, redPacketId, e);
        }

        log.info("【红包落库成功】用户: {} 账目处理完毕", userId);
    }
}
