package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.mapper.AccountLogMapper;
import cn.wyq.serverwebsocket.mapper.AccountMapper;
import cn.wyq.serverwebsocket.mapper.RechargeOrderMapper;
import cn.wyq.serverwebsocket.pojo.entity.Account;
import cn.wyq.serverwebsocket.pojo.entity.AccountLog;
import cn.wyq.serverwebsocket.pojo.entity.RechargeOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class RechargeService {

    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private AccountLogMapper accountLogMapper;
    public boolean checkOrderIsPaid(String outTradeNo) {
        if (outTradeNo == null || outTradeNo.trim().isEmpty()) {
            return false;
        }

        // 1. 去数据库查询该订单的状态
        Integer status = rechargeOrderMapper.selectStatusByOutTradeNo(outTradeNo);

        // 2. 如果订单不存在，返回 false
        if (status == null) {
            return false;
        }

        // 3. 判断状态是否为 1 (1 代表已支付，请根据你数据库的实际设计调整)
        return status == 1;
    }
    /**
     * 1. 扫码前：创建待支付订单
     */
    public void createOrder(String outTradeNo, Integer userId, BigDecimal amount) {
        RechargeOrder order = RechargeOrder.builder()
                .outTradeNo(outTradeNo)
                .userId(userId)
                .amount(amount)
                .status(0) // 0-待支付
                .createTime(LocalDateTime.now())
                .build();
        rechargeOrderMapper.insert(order);
    }

    /**
     * 2. 扫码后：支付宝回调，安全加钱、记流水
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleAlipayNotify(String outTradeNo, String alipayTradeNo, BigDecimal amount) {
        // 1. 查询订单
        RechargeOrder order = rechargeOrderMapper.selectById(outTradeNo);
        if (order == null) {
            log.error("充值订单不存在: {}", outTradeNo);
            return;
        }

        // 2. 幂等性校验：如果订单已经是支付成功状态，直接返回，防止支付宝多次通知导致重复加钱
        if (order.getStatus() == 1) {
            log.info("充值订单 {} 已处理，无需重复加钱", outTradeNo);
            return;
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

        log.info("【充值成功入账】用户: {}, 金额: {}", userId, amount);
    }
}