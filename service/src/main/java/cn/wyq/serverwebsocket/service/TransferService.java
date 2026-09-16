package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.messaging.DelayedExpiryMessage;
import cn.wyq.serverwebsocket.messaging.ExpiryMessagingConstants;
import cn.wyq.serverwebsocket.mapper.AccountLogMapper;
import cn.wyq.serverwebsocket.mapper.AccountMapper;
import cn.wyq.serverwebsocket.mapper.TransferMapper;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.entity.Account;
import cn.wyq.serverwebsocket.pojo.entity.AccountLog;
import cn.wyq.serverwebsocket.pojo.entity.Transfer;
import cn.wyq.serverwebsocket.pojo.entity.UserEmailApplyEntity;
import cn.wyq.serverwebsocket.utils.CacheClient;
import cn.wyq.serverwebsocket.utils.SystemConfigUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TransferService：业务服务组件，提供可复用的领域处理能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferMapper transferMapper;
    private final AccountMapper accountMapper;
    private final AccountLogMapper accountLogMapper;
    private final UserMapper userMapper;
    private final CacheClient cacheClient;
    private final SysConfigService sysConfigService;
    private final DelayedExpiryMessageScheduler expiryMessageScheduler;

    @Transactional(rollbackFor = Exception.class)
    public Transfer sendTransfer(Integer senderId, String receiverName, BigDecimal amount, String remark) {
        if (senderId == null || receiverName == null || receiverName.trim().isEmpty()) {
            throw new IllegalArgumentException("转账对象不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("转账金额必须大于0");
        }

        UserEmailApplyEntity receiver = userMapper.findOneByUsername(receiverName.trim());
        if (receiver == null) {
            throw new IllegalArgumentException("未找到收款用户");
        }
        if (Integer.valueOf(receiver.getId()).equals(senderId)) {
            throw new IllegalArgumentException("不能给自己转账");
        }

        Account sender = accountMapper.selectByUserIdForUpdate(senderId);
        if (sender == null || sender.getBalance() == null || sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("余额不足，无法转账");
        }

        BigDecimal balanceAfter = sender.getBalance().subtract(amount);
        sender.setBalance(balanceAfter);
        accountMapper.updateById(sender);

        String transferNo = "TF" + System.currentTimeMillis()
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        accountLogMapper.insert(AccountLog.builder()
                .userId(senderId)
                .type(7)
                .amount(amount.negate())
                .balanceAfter(balanceAfter)
                .orderNo(transferNo)
                .remark("发起转账")
                .createTime(LocalDateTime.now())
                .build());

        Transfer transfer = Transfer.builder()
                .transferNo(transferNo)
                .senderId(senderId)
                .receiverId(receiver.getId())
                .amount(amount)
                .status(0)
                .remark(remark)
                .createTime(LocalDateTime.now())
                .build();
        transferMapper.insert(transfer);
        // 转账创建成功后先安排第一块延迟检查，后续由 RabbitMQ 消费者递进处理。
        expiryMessageScheduler.scheduleFirst(
                ExpiryMessagingConstants.BUSINESS_TRANSFER,
                String.valueOf(transfer.getId()));
        cacheClient.evict("account:balance", senderId);
        cacheClient.evictByPrefix("finance");
        return transfer;
    }

    @Transactional(rollbackFor = Exception.class)
    public Transfer acceptTransfer(Integer receiverId, String transferNo) {
        if (receiverId == null || transferNo == null || transferNo.trim().isEmpty()) {
            throw new IllegalArgumentException("转账参数不能为空");
        }
        Transfer transfer = transferMapper.selectByTransferNoForUpdate(transferNo);
        if (transfer == null) {
            throw new IllegalArgumentException("转账不存在");
        }
        if (!receiverId.equals(transfer.getReceiverId())) {
            throw new IllegalArgumentException("您不是该转账的收款人");
        }
        if (transfer.getStatus() == null || transfer.getStatus() != 0) {
            throw new IllegalStateException("该转账已处理");
        }
        int expireHours = transferExpireHours();
        if (isExpired(transfer, expireHours)) {
            returnTransferLocked(transfer, expireHours);
            transfer.setStatus(2);
            transfer.setReturnTime(LocalDateTime.now());
            return transfer;
        }

        Account receiver = accountMapper.selectByUserIdForUpdate(receiverId);
        BigDecimal balanceAfter;
        if (receiver == null) {
            balanceAfter = transfer.getAmount();
            accountMapper.insert(Account.builder().userId(receiverId).balance(balanceAfter).build());
        } else {
            balanceAfter = receiver.getBalance().add(transfer.getAmount());
            receiver.setBalance(balanceAfter);
            accountMapper.updateById(receiver);
        }

        if (transferMapper.markTransferAccepted(transfer.getId()) != 1) {
            throw new IllegalStateException("该转账已处理");
        }
        accountLogMapper.insert(AccountLog.builder()
                .userId(receiverId)
                .type(7)
                .amount(transfer.getAmount())
                .balanceAfter(balanceAfter)
                .orderNo(transfer.getTransferNo())
                .remark("收到转账")
                .createTime(LocalDateTime.now())
                .build());
        cacheClient.evict("account:balance", receiverId);
        cacheClient.evict("transfers:detail", transferNo);
        cacheClient.evictByPrefix("finance");
        return transfer;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean refundExpiredTransfer(Long transferId) {
        int expireHours = transferExpireHours();
        Transfer transfer = transferMapper.selectByIdForUpdate(transferId);
        if (transfer == null || transfer.getStatus() == null || transfer.getStatus() != 0
                || !isExpired(transfer, expireHours)) {
            return false;
        }
        returnTransferLocked(transfer, expireHours);
        cacheClient.evict("account:balance", transfer.getSenderId());
        cacheClient.evict("transfers:detail", transfer.getTransferNo());
        cacheClient.evictByPrefix("finance");
        log.info("转账 [{}] 过期退回发送者 [{}]，金额 {}", transfer.getTransferNo(), transfer.getSenderId(), transfer.getAmount());
        return true;
    }

    /** 处理转账到期检查，最后一个时间块才执行原路退回。 */
    @Transactional(rollbackFor = Exception.class)
    public void processExpiryCheck(DelayedExpiryMessage message) {
        Long transferId;
        try {
            transferId = Long.valueOf(message.getBusinessId());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("转账到期消息编号无效", exception);
        }
        Transfer transfer = transferMapper.selectByIdForUpdate(transferId);
        if (transfer == null || transfer.getStatus() == null || transfer.getStatus() != 0) {
            return;
        }
        if (!expiryMessageScheduler.isFinalStep(message)) {
            expiryMessageScheduler.scheduleNext(message);
            return;
        }
        refundTransferAtMessageDeadline(transferId);
    }

    public Transfer getTransfer(String transferNo) {
        Transfer transfer = cacheClient.queryWithMutex("transfers:detail", transferNo,
                Transfer.class, transferMapper::selectByTransferNo,
                30, java.util.concurrent.TimeUnit.SECONDS);
        if (transfer == null) {
            throw new IllegalArgumentException("转账不存在");
        }
        return transfer;
    }

    /** 最后一块 RabbitMQ 消息已经代表原转账的截止时间，不再读取当前过期配置。 */
    private boolean refundTransferAtMessageDeadline(Long transferId) {
        Transfer transfer = transferMapper.selectByIdForUpdate(transferId);
        if (transfer == null || transfer.getStatus() == null || transfer.getStatus() != 0) {
            return false;
        }
        returnTransferLocked(transfer, null);
        cacheClient.evict("account:balance", transfer.getSenderId());
        cacheClient.evict("transfers:detail", transfer.getTransferNo());
        cacheClient.evictByPrefix("finance");
        log.info("转账 [{}] 经 RabbitMQ 分段延迟检查后退回发送者 [{}]，金额 {}",
                transfer.getTransferNo(), transfer.getSenderId(), transfer.getAmount());
        return true;
    }

    private void returnTransferLocked(Transfer transfer, Integer expireHours) {
        int updated = expireHours == null
                ? transferMapper.markTransferReturnedAtDeadline(transfer.getId())
                : transferMapper.markTransferReturned(transfer.getId(), expireHours);
        if (updated != 1) {
            throw new IllegalStateException("该转账已处理");
        }

        Account sender = accountMapper.selectByUserIdForUpdate(transfer.getSenderId());
        BigDecimal balanceAfter;
        if (sender == null) {
            balanceAfter = transfer.getAmount();
            accountMapper.insert(Account.builder().userId(transfer.getSenderId()).balance(balanceAfter).build());
        } else {
            balanceAfter = sender.getBalance().add(transfer.getAmount());
            sender.setBalance(balanceAfter);
            accountMapper.updateById(sender);
        }

        accountLogMapper.insert(AccountLog.builder()
                .userId(transfer.getSenderId())
                .type(6)
                .amount(transfer.getAmount())
                .balanceAfter(balanceAfter)
                .orderNo("TFR" + transfer.getTransferNo())
                .remark("转账过期退回")
                .createTime(LocalDateTime.now())
                .build());
        cacheClient.evict("account:balance", transfer.getSenderId());
        cacheClient.evict("transfers:detail", transfer.getTransferNo());
        cacheClient.evictByPrefix("finance");
    }

    /** 从平台配置读取转账过期窗口，配置异常时回退到一天。 */
    private int transferExpireHours() {
        try {
            return SystemConfigUtils.intValue(sysConfigService.getConfigMap(),
                    "transfer_expire_hours", 24, 1, 24 * 365);
        } catch (RuntimeException e) {
            log.warn("读取转账过期配置失败，将使用默认 24 小时", e);
            return 24;
        }
    }

    private boolean isExpired(Transfer transfer, int expireHours) {
        return transfer == null || transfer.getCreateTime() == null
                || !transfer.getCreateTime().isAfter(LocalDateTime.now().minusHours(expireHours));
    }
}
