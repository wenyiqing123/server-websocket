package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.mapper.LoginAuditLogMapper;
import cn.wyq.serverwebsocket.messaging.LoginAuditMessage;
import cn.wyq.serverwebsocket.pojo.entity.LoginAuditLog;
import cn.wyq.serverwebsocket.pojo.vo.LoginAuditQueryDTO;
import cn.wyq.serverwebsocket.pojo.vo.LoginAuditStatsVO;
import cn.wyq.serverwebsocket.service.LoginAuditService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 登录审计日志业务实现。 */
@Service
@RequiredArgsConstructor
public class LoginAuditServiceImpl implements LoginAuditService {
    private final LoginAuditLogMapper mapper;

    @Override
    public void save(LoginAuditMessage message) {
        if (message == null || message.getEventId() == null || message.getEventId().isBlank()) {
            return;
        }
        mapper.insertIgnore(LoginAuditLog.builder()
                .eventId(message.getEventId())
                .userId(message.getUserId())
                .username(message.getUsername())
                .eventType(message.getEventType())
                .success(message.getSuccess())
                .remoteLogin(message.getRemoteLogin())
                .previousIp(message.getPreviousIp())
                .ipAddress(message.getIpAddress())
                .userAgent(message.getUserAgent())
                .failureReason(message.getFailureReason())
                .occurredAt(message.getOccurredAt() == null ? LocalDateTime.now() : message.getOccurredAt())
                .build());
    }

    @Override
    public PageResult<List<LoginAuditLog>> page(LoginAuditQueryDTO query) {
        LoginAuditQueryDTO actual = query == null ? new LoginAuditQueryDTO() : query;
        int pageNum = actual.getPageNum() == null || actual.getPageNum() < 1 ? 1 : actual.getPageNum();
        int pageSize = actual.getPageSize() == null || actual.getPageSize() < 1 ? 15 : Math.min(actual.getPageSize(), 200);
        PageHelper.startPage(pageNum, pageSize);
        List<LoginAuditLog> rows = mapper.selectPage(actual);
        PageInfo<LoginAuditLog> page = new PageInfo<>(rows);
        return PageResult.success(page.getTotal(), page.getList());
    }

    @Override
    public LoginAuditStatsVO stats(LoginAuditQueryDTO query) {
        return mapper.selectStats(query == null ? new LoginAuditQueryDTO() : query);
    }
}
