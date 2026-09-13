package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.messaging.LoginAuditMessage;
import cn.wyq.serverwebsocket.pojo.entity.LoginAuditLog;
import cn.wyq.serverwebsocket.pojo.vo.LoginAuditQueryDTO;
import cn.wyq.serverwebsocket.pojo.vo.LoginAuditStatsVO;

import java.util.List;

/** 登录审计日志业务接口。 */
public interface LoginAuditService {
    void save(LoginAuditMessage message);

    PageResult<List<LoginAuditLog>> page(LoginAuditQueryDTO query);

    LoginAuditStatsVO stats(LoginAuditQueryDTO query);
}
