package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.AccountLogMapper;
import cn.wyq.serverwebsocket.mapper.AccountMapper;
import cn.wyq.serverwebsocket.pojo.entity.Account;
import cn.wyq.serverwebsocket.pojo.entity.AccountLog;
import cn.wyq.serverwebsocket.utils.CacheClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * AccountService：业务服务组件，提供可复用的领域处理能力。
 */
@Slf4j
@Service
public class AccountService {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AccountLogMapper accountLogMapper;
    @Autowired
    private CacheClient cacheClient;

    @Transactional(rollbackFor = Exception.class)
    public BigDecimal recharge(Integer userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("充值金额必须大于0");
        }

        log.info("用户 [{}] 开始充值，金额: {}", userId, amount);

        // 1. 悲观锁查询账户 (复用你之前在 AccountMapper 写好的方法)
        Account account = accountMapper.selectByUserIdForUpdate(userId);
        BigDecimal balanceAfter;

        if (account != null) {
            // 老用户：累加余额
            balanceAfter = account.getBalance().add(amount);
            account.setBalance(balanceAfter);
            accountMapper.updateById(account);
        } else {
            // 新用户：初始化钱包
            balanceAfter = amount;
            account = Account.builder()
                    .userId(userId)
                    .balance(balanceAfter)
                    .build();
            accountMapper.insert(account);
        }

        // 2. 记录资金流水 (类型：1-充值)
        String orderNo = "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);
        AccountLog accountLog = AccountLog.builder()
                .userId(userId)
                .type(1) // 🌟 变动类型：1-充值
                .amount(amount)
                .balanceAfter(balanceAfter)
                .orderNo(orderNo)
                .remark("系统模拟充值")
                .createTime(LocalDateTime.now())
                .build();
        accountLogMapper.insert(accountLog);

        log.info("用户 [{}] 充值成功，当前余额: {}", userId, balanceAfter);
        cacheClient.evict("account:balance", userId);
        cacheClient.evictByPrefix("finance");
        return balanceAfter;
    }
    public BigDecimal getCurrentUserBalance(Long userId) {
        if (userId == null) {
            throw new ServiceException("无法获取用户信息，请重新登录", 401);
        }

        return cacheClient.queryWithMutex("account:balance", userId, BigDecimal.class,
                id -> {
                    BigDecimal balance = accountMapper.selectBalanceByUserId(id);
                    return balance != null ? balance : BigDecimal.ZERO;
                }, 30, TimeUnit.SECONDS);
    }
}
