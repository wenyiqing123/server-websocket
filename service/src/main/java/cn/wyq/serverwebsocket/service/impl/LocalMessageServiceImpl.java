package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.mapper.LocalMessageLogMapper;
import cn.wyq.serverwebsocket.pojo.entity.LocalMessageLog;
import cn.wyq.serverwebsocket.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 本地消息表服务实现。 */
@Service
@RequiredArgsConstructor
public class LocalMessageServiceImpl implements LocalMessageService {
    private static final String STATUS_PENDING = "PENDING";

    private final LocalMessageLogMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String enqueue(String messageType, String exchangeName, String routingKey, String payload) {
        return enqueueWithMessageId(UUID.randomUUID().toString(), messageType, exchangeName, routingKey, payload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String enqueueWithMessageId(String messageId, String messageType, String exchangeName,
                                       String routingKey, String payload) {
        if (isBlank(messageId) || isBlank(messageType) || isBlank(exchangeName)
                || isBlank(routingKey) || payload == null) {
            throw new IllegalArgumentException("本地消息参数不完整");
        }
        mapper.insertPendingIdempotent(LocalMessageLog.builder()
                .messageId(messageId)
                .messageType(messageType)
                .exchangeName(exchangeName)
                .routingKey(routingKey)
                .payload(payload)
                .status(STATUS_PENDING)
                .attemptCount(0)
                .nextRetryAt(LocalDateTime.now())
                .build());
        return messageId;
    }

    @Override
    public List<LocalMessageLog> findDispatchable(int limit) {
        return mapper.selectDispatchable(Math.max(1, Math.min(limit, 500)));
    }

    @Override
    public LocalMessageLog findByMessageId(String messageId) {
        return isBlank(messageId) ? null : mapper.selectByMessageId(messageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claim(Long id) {
        return mapper.claim(id) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resetStaleSending(int staleMinutes) {
        return mapper.resetStaleSending(Math.max(1, Math.min(staleMinutes, 1440)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markConfirmed(String messageId) {
        return !isBlank(messageId) && mapper.markConfirmed(messageId) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markRetry(String messageId, String errorMessage, int delaySeconds) {
        return !isBlank(messageId)
                && mapper.markRetry(messageId, trimError(errorMessage), safeDelay(delaySeconds)) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markReturned(String messageId, String errorMessage, int delaySeconds) {
        return !isBlank(messageId)
                && mapper.markReturned(messageId, trimError(errorMessage), safeDelay(delaySeconds)) == 1;
    }

    private int safeDelay(int delaySeconds) {
        return Math.max(1, Math.min(delaySeconds, 3600));
    }

    private String trimError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "未提供错误原因";
        }
        return errorMessage.length() <= 2000 ? errorMessage : errorMessage.substring(0, 2000);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
