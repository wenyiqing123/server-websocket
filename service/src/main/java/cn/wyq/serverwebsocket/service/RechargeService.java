package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.messaging.DelayedExpiryMessage;
import cn.wyq.serverwebsocket.messaging.ExpiryMessagingConstants;
import cn.wyq.serverwebsocket.mapper.AccountLogMapper;
import cn.wyq.serverwebsocket.mapper.AccountMapper;
import cn.wyq.serverwebsocket.mapper.RechargeOrderMapper;
import cn.wyq.serverwebsocket.pojo.entity.Account;
import cn.wyq.serverwebsocket.pojo.entity.AccountLog;
import cn.wyq.serverwebsocket.pojo.entity.RechargeOrder;
import cn.wyq.serverwebsocket.utils.CacheClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RechargeService：业务服务组件，提供可复用的领域处理能力。
 */
@Slf4j
@Service
public class RechargeService {

    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private AccountLogMapper accountLogMapper;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private DelayedExpiryMessageScheduler expiryMessageScheduler;
    public boolean checkOrderIsPaid(String outTradeNo) {
        if (outTradeNo == null || outTradeNo.trim().isEmpty()) {
            return false;
        }
        // 支付状态不能缓存：支付宝回调提交后，前端必须立即看到最新状态。
        Integer status = rechargeOrderMapper.selectStatusByOutTradeNo(outTradeNo);
        return Integer.valueOf(1).equals(status);
    }

    /**
     * 查询当前用户的充值订单，避免仅凭订单号查询到其他用户的支付状态。
     */
    public RechargeOrder findOrderForUser(String outTradeNo, Integer userId) {
        if (outTradeNo == null || outTradeNo.isBlank() || userId == null) {
            return null;
        }
        RechargeOrder order = rechargeOrderMapper.selectById(outTradeNo);
        return order != null && userId.equals(order.getUserId()) ? order : null;
    }
    /**
     * 1. 扫码前：创建待支付订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(String outTradeNo, Integer userId, BigDecimal amount) {
        RechargeOrder order = RechargeOrder.builder()
                .outTradeNo(outTradeNo)
                .userId(userId)
                .amount(amount)
                .status(0) // 0-待支付
                .createTime(LocalDateTime.now())
                .build();
        rechargeOrderMapper.insert(order);
        // 订单和第一条延迟检查消息在同一个事务中落库，避免只创建订单却没有取消保障。
        expiryMessageScheduler.scheduleFirst(
                ExpiryMessagingConstants.BUSINESS_RECHARGE_ORDER, outTradeNo);
        cacheClient.evict("finance:recharge-status", outTradeNo);
    }

    /**
     * 处理充值订单到期检查：非最后时间块只安排下一次检查，最后一块才关闭未支付订单。
     */
    @Transactional(rollbackFor = Exception.class)
    public void processExpiryCheck(DelayedExpiryMessage message) {
        RechargeOrder order = rechargeOrderMapper.selectOrderForUpdate(message.getBusinessId());
        if (order == null || !Integer.valueOf(0).equals(order.getStatus())) {
            return;
        }
        if (!expiryMessageScheduler.isFinalStep(message)) {
            expiryMessageScheduler.scheduleNext(message);
            return;
        }
        if (rechargeOrderMapper.updateOrderStatusToClosed(order.getOutTradeNo()) > 0) {
            cacheClient.evict("finance:recharge-status", order.getOutTradeNo());
            cacheClient.evictByPrefix("finance");
            log.info("充值订单 [{}] 经 RabbitMQ 分段延迟检查后自动关闭", order.getOutTradeNo());
        }
    }

    /**
     * 2. 扫码后：支付宝回调，安全加钱、记流水
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleAlipayNotify(String outTradeNo, String alipayTradeNo, BigDecimal amount) {
        // 1. 锁定订单，支付宝可能对同一订单发送多次并发通知。
        RechargeOrder order = rechargeOrderMapper.selectOrderForUpdate(outTradeNo);
        if (order == null) {
            log.error("充值订单不存在: {}", outTradeNo);
            return;
        }

        // 2. 幂等性校验：如果订单已经是支付成功状态，直接返回，防止支付宝多次通知导致重复加钱
        if (Integer.valueOf(1).equals(order.getStatus())) {
            log.info("充值订单 {} 已处理，无需重复加钱", outTradeNo);
            return;
        }
        if (!Integer.valueOf(0).equals(order.getStatus())) {
            throw new IllegalStateException("充值订单已关闭，拒绝入账: " + outTradeNo);
        }
        if (amount == null || order.getAmount() == null || order.getAmount().compareTo(amount) != 0) {
            throw new IllegalArgumentException("支付宝回调金额与订单金额不一致: " + outTradeNo);
        }
        if (alipayTradeNo == null || alipayTradeNo.isBlank()) {
            throw new IllegalArgumentException("支付宝交易号为空: " + outTradeNo);
        }

        Integer userId = order.getUserId();

        // 3. 悲观锁更新账户余额 (复用你的 Account 逻辑)
        Account account = accountMapper.selectByUserIdForUpdate(userId);
        BigDecimal balanceAfter;
        if (account != null) {
            balanceAfter = account.getBalance().add(amount);
            account.setBalance(balanceAfter);
            accountMapper.updateById(account);
        } else {
            balanceAfter = amount;
            account = Account.builder()
                    .userId(userId)
                    .balance(balanceAfter)
                    .build();
            accountMapper.insert(account);
        }

        // 4. 记录资金流水 (Type=1 表示充值)
        String logOrderNo = "RCG" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);
        AccountLog accountLog = AccountLog.builder()
                .userId(userId)
                .type(1) // 1-支付宝充值进账
                .amount(amount)
                .balanceAfter(balanceAfter)
                .orderNo(logOrderNo)
                .remark("支付宝沙盒充值")
                .createTime(LocalDateTime.now())
                .build();
        accountLogMapper.insert(accountLog);

        // 5. 更新充值订单状态
        order.setStatus(1); // 1-已支付
        order.setAlipayTradeNo(alipayTradeNo);
        order.setPayTime(LocalDateTime.now());
        rechargeOrderMapper.updateById(order);
        // 事务提交前不能删除缓存，否则并发查询可能把未提交的旧数据重新写回 Redis。
        evictPaymentCachesAfterCommit(outTradeNo, userId);

        log.info("【充值成功入账】用户: {}, 金额: {}", userId, amount);
    }

    /** 只有数据库事务提交后才失效支付状态、余额及管理端财务缓存。 */
    private void evictPaymentCachesAfterCommit(String outTradeNo, Integer userId) {
        Runnable eviction = () -> {
            cacheClient.evict("finance:recharge-status", outTradeNo);
            cacheClient.evict("account:balance", userId);
            cacheClient.evictByPrefix("finance");
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eviction.run();
                }
            });
        } else {
            eviction.run();
        }
    }
}
