package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.mapper.AccountLogMapper;
import cn.wyq.serverwebsocket.mapper.AccountMapper;
import cn.wyq.serverwebsocket.mapper.TransferMapper;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.entity.Account;
import cn.wyq.serverwebsocket.pojo.entity.AccountLog;
import cn.wyq.serverwebsocket.pojo.entity.Transfer;
import cn.wyq.serverwebsocket.pojo.entity.UserEmailApplyEntity;
import cn.wyq.serverwebsocket.utils.CacheClient;
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
        if (isExpired(transfer)) {
            returnTransferLocked(transfer);
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
        Transfer transfer = transferMapper.selectByIdForUpdate(transferId);
        if (transfer == null || transfer.getStatus() == null || transfer.getStatus() != 0 || !isExpired(transfer)) {
            return false;
        }
        returnTransferLocked(transfer);
        cacheClient.evict("account:balance", transfer.getSenderId());
        cacheClient.evict("transfers:detail", transfer.getTransferNo());
        cacheClient.evictByPrefix("finance");
        log.info("转账 [{}] 过期退回发送者 [{}]，金额 {}", transfer.getTransferNo(), transfer.getSenderId(), transfer.getAmount());
        return true;
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

    private void returnTransferLocked(Transfer transfer) {
        if (transferMapper.markTransferReturned(transfer.getId()) != 1) {
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

    private boolean isExpired(Transfer transfer) {
        return transfer.getCreateTime() == null
                || !transfer.getCreateTime().isAfter(LocalDateTime.now().minusDays(1));
    }
}
