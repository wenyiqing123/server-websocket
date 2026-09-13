package cn.wyq.serverwebsocket.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 登录审计统计数据。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginAuditStatsVO {
    private Long total;
    private Long success;
    private Long failure;
    private Long remoteLogin;
    private Long today;
}
