package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.LoginAuditLog;
import cn.wyq.serverwebsocket.pojo.vo.LoginAuditQueryDTO;
import cn.wyq.serverwebsocket.pojo.vo.LoginAuditStatsVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** 登录审计日志数据访问接口。 */
@Mapper
public interface LoginAuditLogMapper {
    int insertIgnore(LoginAuditLog log);

    List<LoginAuditLog> selectPage(LoginAuditQueryDTO query);

    LoginAuditStatsVO selectStats(LoginAuditQueryDTO query);
}
