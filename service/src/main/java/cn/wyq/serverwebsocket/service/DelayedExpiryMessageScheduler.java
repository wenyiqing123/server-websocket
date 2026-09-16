package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.messaging.DelayedExpiryMessage;
import cn.wyq.serverwebsocket.messaging.ExpiryMessagingConstants;
import cn.wyq.serverwebsocket.utils.SystemConfigUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 资金到期检查消息调度器。
 *
 * <p>该组件只负责把下一次检查写入本地消息表，RabbitMQ 投递由现有可靠消息调度器完成。
 * 因此创建订单、红包或转账的数据库事务提交后，第一块延迟消息也会和业务数据同生共死。</p>
 */
@Service
public class DelayedExpiryMessageScheduler {

    private final LocalMessageService localMessageService;
    private final ObjectMapper objectMapper;
    private final SysConfigService sysConfigService;
    private final String configuredRechargeSchedule;

    public DelayedExpiryMessageScheduler(LocalMessageService localMessageService,
                                         ObjectMapper objectMapper,
                                         SysConfigService sysConfigService,
                                         @Value("${app.messaging.expiry.recharge-delay-seconds:}")
                                         String configuredRechargeSchedule) {
        this.localMessageService = localMessageService;
        this.objectMapper = objectMapper;
        this.sysConfigService = sysConfigService;
        this.configuredRechargeSchedule = configuredRechargeSchedule;
    }

    /** 创建业务记录后投递第一个时间块。 */
    public void scheduleFirst(String businessType, String businessId) {
        List<Integer> schedule = resolveSchedule(businessType);
        enqueue(businessType, businessId, 0, schedule);
    }

    /**
     * 消费当前检查消息后投递下一时间块。
     *
     * @return 当前消息后是否还有下一块
     */
    public boolean scheduleNext(DelayedExpiryMessage current) {
        validate(current);
        int nextStep = current.getStep() + 1;
        if (nextStep >= current.getDelaySchedule().size()) {
            return false;
        }
        enqueue(current.getBusinessType(), current.getBusinessId(), nextStep,
                current.getDelaySchedule());
        return true;
    }

    /** 判断当前消息是否已经是最后一个时间块。 */
    public boolean isFinalStep(DelayedExpiryMessage message) {
        validate(message);
        return message.getStep() != null
                && message.getStep() >= message.getDelaySchedule().size() - 1;
    }

    /** 校验从 RabbitMQ 反序列化出的时间块消息，避免非法 step 被误当成最后一块直接退款。 */
    public void validate(DelayedExpiryMessage message) {
        if (message == null || message.getBusinessType() == null || message.getBusinessType().isBlank()
                || message.getBusinessId() == null || message.getBusinessId().isBlank()
                || message.getStep() == null || message.getDelaySchedule() == null
                || message.getDelaySchedule().isEmpty()
                || message.getStep() < 0 || message.getStep() >= message.getDelaySchedule().size()) {
            throw new IllegalArgumentException("到期检查消息缺少有效时间块信息");
        }
        ExpiryMessagingConstants.delayRoutingKey(message.getBusinessType());
        for (Integer seconds : message.getDelaySchedule()) {
            if (seconds == null || seconds <= 0) {
                throw new IllegalArgumentException("到期检查消息包含无效延迟时间");
            }
        }
        if (message.getDelaySeconds() == null || message.getDelaySeconds() <= 0) {
            throw new IllegalArgumentException("到期检查消息缺少当前延迟时间");
        }
    }

    private void enqueue(String businessType, String businessId, int step, List<Integer> schedule) {
        if (businessType == null || businessType.isBlank()
                || businessId == null || businessId.isBlank()
                || schedule == null || step < 0 || step >= schedule.size()) {
            throw new IllegalArgumentException("到期消息参数不完整");
        }
        int delaySeconds = Math.max(1, schedule.get(step));
        DelayedExpiryMessage message = DelayedExpiryMessage.builder()
                .businessType(businessType)
                .businessId(businessId)
                .step(step)
                .delaySchedule(List.copyOf(schedule))
                .delaySeconds(delaySeconds)
                .build();
        try {
            String payload = objectMapper.writeValueAsString(message);
            String messageId = stableMessageId(businessType, businessId, step);
            localMessageService.enqueueWithMessageId(
                    messageId,
                    ExpiryMessagingConstants.MESSAGE_TYPE,
                    ExpiryMessagingConstants.EXCHANGE,
                    ExpiryMessagingConstants.delayRoutingKey(businessType),
                    payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("到期检查消息序列化失败", exception);
        }
    }

    /**
     * 充值默认沿用后台的 recharge_close_minutes 配置；若配置为 1 分钟，
     * 三个时间块就是 10 秒、20 秒、30 秒。也支持通过环境变量覆盖完整数组。
     */
    private List<Integer> resolveSchedule(String businessType) {
        if (ExpiryMessagingConstants.BUSINESS_RECHARGE_ORDER.equals(businessType)
                && configuredRechargeSchedule != null && !configuredRechargeSchedule.isBlank()) {
            List<Integer> configured = parseSchedule(configuredRechargeSchedule);
            if (!configured.isEmpty()) {
                return configured;
            }
        }

        Map<String, String> config = sysConfigService.getConfigMap();
        int totalSeconds;
        if (ExpiryMessagingConstants.BUSINESS_RECHARGE_ORDER.equals(businessType)) {
            int minutes = SystemConfigUtils.intValue(config,
                    "recharge_close_minutes", 5, 1, 24 * 60);
            totalSeconds = minutes * 60;
        } else if (ExpiryMessagingConstants.BUSINESS_RED_PACKET.equals(businessType)) {
            int hours = SystemConfigUtils.intValue(config,
                    "red_packet_expire_hours", 24, 1, 24 * 365);
            totalSeconds = hours * 3600;
        } else if (ExpiryMessagingConstants.BUSINESS_TRANSFER.equals(businessType)) {
            int hours = SystemConfigUtils.intValue(config,
                    "transfer_expire_hours", 24, 1, 24 * 365);
            totalSeconds = hours * 3600;
        } else {
            throw new IllegalArgumentException("不支持的到期业务类型：" + businessType);
        }
        return splitIntoThreeBlocks(totalSeconds);
    }

    private List<Integer> parseSchedule(String value) {
        List<Integer> schedule = new ArrayList<>();
        for (String item : value.split(",")) {
            try {
                int seconds = Integer.parseInt(item.trim());
                if (seconds > 0 && seconds <= 365 * 24 * 3600) {
                    schedule.add(seconds);
                }
            } catch (NumberFormatException ignored) {
                // 忽略非法片段，全部非法时回退到数据库配置。
            }
        }
        return schedule;
    }

    /** 按 1:2:3 切分总时长，最后一块吸收除不尽的余数。 */
    private List<Integer> splitIntoThreeBlocks(int totalSeconds) {
        int total = Math.max(3, totalSeconds);
        int first = Math.max(1, total / 6);
        int second = Math.max(1, total / 3);
        int third = Math.max(1, total - first - second);
        return List.of(first, second, third);
    }

    private String stableMessageId(String businessType, String businessId, int step) {
        String identity = businessType + ":" + businessId + ":" + step;
        return "expiry-" + UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
    }

}
