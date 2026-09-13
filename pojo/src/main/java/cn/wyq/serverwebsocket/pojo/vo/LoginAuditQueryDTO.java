package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

/** 登录审计日志查询条件。 */
@Data
public class LoginAuditQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 15;
    private Integer userId;
    private String username;
    private String eventType;
    private Boolean success;
    private Boolean remoteLogin;
    private String startDate;
    private String endDate;
}
