package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.component.RedPacketAsyncComponent;
import cn.wyq.serverwebsocket.constant.RedPacketConstant;
import cn.wyq.serverwebsocket.mapper.*;
import cn.wyq.serverwebsocket.pojo.entity.*;
import cn.wyq.serverwebsocket.pojo.vo.RedPacketDetailVO;
import cn.wyq.serverwebsocket.utils.RedPacketUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RedPacketService {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AccountLogMapper accountLogMapper;

    @Autowired
    private RedPacketMapper redPacketMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedPacketAsyncComponent redPacketAsyncComponent;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedPacketRecordMapper redPacketRecordMapper;

    /**
     * 查询红包详情与领取列表
     */
    public RedPacketDetailVO getRedPacketDetail(Long redPacketId, Integer currentUserId) {
        RedPacketDetailVO vo = new RedPacketDetailVO();

        // 1. 查询红包主记录
        RedPacket redPacket = redPacketMapper.selectById(redPacketId);
        if (redPacket == null) {
            throw new RuntimeException("该红包已过期或不存在");
        }

        // 2. 查询发送者信息
        UserEmailApplyEntity sender = userMapper.getCurrentUserById(redPacket.getSenderId());

        vo.setRedPacketId(redPacket.getId());
        vo.setSenderId(redPacket.getSenderId());
        vo.setSenderName(sender != null ? sender.getUserName() : "未知用户");
        vo.setSenderAvatar(sender != null ? sender.getPath() : "");
        vo.setBlessing("恭喜发财，大吉大利");
        vo.setTotalAmount(redPacket.getTotalAmount());
        vo.setTotalCount(redPacket.getTotalCount());
        vo.setRemainingAmount(redPacket.getRemainingAmount());
        vo.setRemainingCount(redPacket.getRemainingCount());
        vo.setStatus(redPacket.getStatus());

        // 3. 使用写好的强力 SQL，一次性查出所有领取明细 + 用户信息
        List<RedPacketDetailVO.RecordItem> recordList = redPacketRecordMapper.getRecordListByRedPacketId(redPacketId);

        // 4. 高级业务计算：寻找自己的金额 & 判定手气最佳
        BigDecimal maxAmount = BigDecimal.ZERO;

        // 只有当红包被全部抢完时，才计算手气最佳
        boolean isGrabbedAll = (redPacket.getRemainingCount() == 0);

        for (RedPacketDetailVO.RecordItem item : recordList) {
            // A. 判定是否有我的份
            if (item.getUserId().equals(currentUserId)) {
                vo.setMyAmount(item.getAmount());
            }

            // B. 找寻全场最高金额
            if (isGrabbedAll && item.getAmount().compareTo(maxAmount) > 0) {
                maxAmount = item.getAmount();
            }
        }

        // C. 给金额最高的那个人（或多个人）打上手气最佳的皇冠
        if (isGrabbedAll && maxAmount.compareTo(BigDecimal.ZERO) > 0) {
            for (RedPacketDetailVO.RecordItem item : recordList) {
                if (item.getAmount().compareTo(maxAmount) == 0) {
                    item.setIsBestLuck(true);
                    break; // 即使有并列第一，通常微信也只会给第一个抢到最高金额的人颁发“手气最佳”
                }
            }
        }

        vo.setRecordList(recordList);
        return vo;
    }

    /**
     * 发送红包核心业务逻辑
     * 🌟 新增参数：receiverId (如果发给群聊，传 null；发给个人，传对方的 userId)
     */
    @Transactional(rollbackFor = Exception.class)
    public Long sendRedPacket(Integer senderId, String groupId, BigDecimal totalAmount, Integer totalCount, String receiverName) {
        log.info("用户 [{}] 开始发送红包，金额: {}, 个数: {}, 群组ID: {}, 专属接收人: {}", senderId, totalAmount, totalCount, groupId, receiverName);

        // 🌟 1. 判断红包类型并获取 receiverId
        Integer receiverId = null; // 默认为空，代表没有专属接收人（即群红包）
        boolean isExclusive = "0".equals(groupId) && StringUtils.isNotBlank(receiverName);

        if (isExclusive) {
            // 如果是专属红包，查询接收人信息
            UserEmailApplyEntity receiverEntity = userMapper.findOneByUsername(receiverName);
            if (receiverEntity == null) {
                throw new RuntimeException("发送失败：未找到指定的专属接收人");
            }
            receiverId = receiverEntity.getId();
        } else if (StringUtils.isBlank(groupId)) {
            // 参数合法性兜底校验：既不是专属红包，群ID又为空，则拒绝发送
            throw new IllegalArgumentException("参数异常：群红包必须指定群组ID");
        }

        // 2. 悲观锁查询账户余额
        Account account = accountMapper.selectByUserIdForUpdate(senderId);
        if (account == null) {
            throw new RuntimeException("账户异常：未找到该用户钱包");
        }

        // 3. 校验余额是否充足
        if (account.getBalance().compareTo(totalAmount) < 0) {
            throw new RuntimeException("余额不足，无法发送红包");
        }

        // 4. 扣减余额并更新入库
        BigDecimal balanceAfter = account.getBalance().subtract(totalAmount);
        account.setBalance(balanceAfter);
        accountMapper.updateById(account);

        // 5. 生成业务单号，记录资金流水 (发红包记为负数出账)
        String orderNo = "RP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);
        AccountLog accountLog = AccountLog.builder()
                .userId(senderId)
                .type(2) // 变动类型：2-发红包
                .amount(totalAmount.negate())
                .balanceAfter(balanceAfter)
                .orderNo(orderNo)
                // 根据判空结果写入动态备注
                .remark(isExclusive ? "发出专属红包" : "发出群聊红包")
                .createTime(LocalDateTime.now())
                .build();
        accountLogMapper.insert(accountLog);

        // 6. 创建红包主记录并入库
        RedPacket redPacket = RedPacket.builder()
                .senderId(senderId)
                .groupId(groupId)
                .totalAmount(totalAmount)
                .totalCount(totalCount)
                .remainingAmount(totalAmount)
                .remainingCount(totalCount)
                .status(0) // 0-进行中
                // 🌟 记录接收人。如果是群聊红包这里为 null；专属红包则是具体用户 ID
                .receiverId(receiverId)
                .createTime(LocalDateTime.now())
                .build();
        redPacketMapper.insert(redPacket);

        // 7. 调用二倍均值算法拆分红包
        List<BigDecimal> splitAmounts = RedPacketUtils.splitRedPacket(totalAmount, totalCount);
        log.info("红包拆分完毕，共 {} 份", splitAmounts.size());

        // 8. 将拆分后的金额全部转为 String 数组
        String[] amountArray = splitAmounts.stream()
                .map(BigDecimal::toString)
                .toArray(String[]::new);

        // 9. 存入 Redis 队列
        String redisListKey = RedPacketConstant.REDIS_KEY_PREFIX + redPacket.getId() + RedPacketConstant.REDIS_KEY_LIST_SUFFIX;
        stringRedisTemplate.opsForList().rightPushAll(redisListKey, amountArray);

        // 10. 在 Redis 中单独存一个该红包的“剩余可抢个数”
        String redisCountKey = RedPacketConstant.REDIS_KEY_PREFIX + redPacket.getId() + RedPacketConstant.REDIS_KEY_COUNT_SUFFIX;
        stringRedisTemplate.opsForValue().set(redisCountKey, String.valueOf(totalCount));

        // 11. 🌟 如果是专属红包，在 Redis 中额外记录一条专属接收人标识
        if (isExclusive) {
            String redisReceiverKey = RedPacketConstant.REDIS_KEY_PREFIX + redPacket.getId() + ":receiver";
            stringRedisTemplate.opsForValue().set(redisReceiverKey, String.valueOf(receiverId));
        }

        log.info("红包 [{}] 预热进 Redis 成功，Key为: {}", redPacket.getId(), redisListKey);

        return redPacket.getId();
    }

    /**
     * 抢红包核心逻辑 (Redis纯内存挡流 + 同步写明细 + 异步落库进账)
     */
    @Transactional(rollbackFor = Exception.class) // 添加强事务，保证同步写明细和Redis的一致性
    public BigDecimal grabRedPacket(Integer userId, Long redPacketId, String groupId) {

        String redisListKey = RedPacketConstant.REDIS_KEY_PREFIX + redPacketId + RedPacketConstant.REDIS_KEY_LIST_SUFFIX;
        String redisSetKey = RedPacketConstant.REDIS_KEY_PREFIX + redPacketId + RedPacketConstant.REDIS_KEY_GRABBED_SET_SUFFIX;
        String redisReceiverKey = RedPacketConstant.REDIS_KEY_PREFIX + redPacketId + ":receiver";

        // 拦截专属红包 (String.valueOf 兼容了 Integer)
        String targetReceiverStr = stringRedisTemplate.opsForValue().get(redisReceiverKey);
        if (targetReceiverStr != null && !targetReceiverStr.equals(String.valueOf(userId))) {
            throw new RuntimeException("这是专属红包，您不能领取哦！");
        }


        // 🌟 原子防重复抢 (绝对安全)
        Long result = stringRedisTemplate.opsForSet().add(redisSetKey, String.valueOf(userId));
        if (result == null || result == 0) {
            throw new RuntimeException("您已经抢过这个红包啦！");
        }

        // 弹出金额
        String amountStr = stringRedisTemplate.opsForList().leftPop(redisListKey);
        if (amountStr == null) {
            stringRedisTemplate.opsForSet().remove(redisSetKey, String.valueOf(userId));
            throw new RuntimeException("手慢了，红包派完了！");
        }

        BigDecimal grabbedAmount = new BigDecimal(amountStr);

        try {
            RedPacketRecord record = RedPacketRecord.builder()
                    .redPacketId(redPacketId)
                    .userId(userId)
                    .amount(grabbedAmount)
                    .createTime(LocalDateTime.now())
                    .build();
            redPacketRecordMapper.insert(record);

            redPacketAsyncComponent.executeGrabDbUpdate(userId, redPacketId, grabbedAmount, groupId);
        } catch (RuntimeException e) {
            RedPacket current = redPacketMapper.selectById(redPacketId);
            if (current != null && current.getStatus() != null && current.getStatus() == 0
                    && current.getCreateTime() != null
                    && current.getCreateTime().isAfter(LocalDateTime.now().minusDays(1))) {
                stringRedisTemplate.opsForList().leftPush(redisListKey, amountStr);
            }
            stringRedisTemplate.opsForSet().remove(redisSetKey, String.valueOf(userId));
            throw e;
        }

        return grabbedAmount;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean refundExpiredRedPacket(Long redPacketId) {
        RedPacket redPacket = redPacketMapper.selectByIdForUpdate(redPacketId);
        if (redPacket == null || redPacket.getStatus() == null || redPacket.getStatus() != 0
                || redPacket.getRemainingAmount() == null
                || redPacket.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0
                || redPacket.getCreateTime() == null
                || redPacket.getCreateTime().isAfter(LocalDateTime.now().minusDays(1))) {
            return false;
        }

        BigDecimal refundAmount = redPacket.getRemainingAmount();
        if (redPacketMapper.markRedPacketExpired(redPacketId) != 1) {
            return false;
        }

        Account account = accountMapper.selectByUserIdForUpdate(redPacket.getSenderId());
        BigDecimal balanceAfter;
        if (account == null) {
            balanceAfter = refundAmount;
            accountMapper.insert(Account.builder()
                    .userId(redPacket.getSenderId())
                    .balance(balanceAfter)
                    .build());
        } else {
            balanceAfter = account.getBalance().add(refundAmount);
            account.setBalance(balanceAfter);
            accountMapper.updateById(account);
        }

        String orderNo = "RPR" + redPacketId + "_" + UUID.randomUUID().toString().substring(0, 8);
        accountLogMapper.insert(AccountLog.builder()
                .userId(redPacket.getSenderId())
                .type(6)
                .amount(refundAmount)
                .balanceAfter(balanceAfter)
                .orderNo(orderNo)
                .remark("红包过期退回")
                .createTime(LocalDateTime.now())
                .build());

        clearRedPacketCache(redPacketId);
        log.info("红包 [{}] 过期退回发送者 [{}]，金额 {}", redPacketId, redPacket.getSenderId(), refundAmount);
        return true;
    }

    private void clearRedPacketCache(Long redPacketId) {
        String keyPrefix = RedPacketConstant.REDIS_KEY_PREFIX + redPacketId;
        stringRedisTemplate.delete(List.of(
                keyPrefix + RedPacketConstant.REDIS_KEY_LIST_SUFFIX,
                keyPrefix + RedPacketConstant.REDIS_KEY_COUNT_SUFFIX,
                keyPrefix + RedPacketConstant.REDIS_KEY_GRABBED_SET_SUFFIX,
                keyPrefix + RedPacketConstant.REDIS_KEY_RECEIVER_SUFFIX));
    }
}
