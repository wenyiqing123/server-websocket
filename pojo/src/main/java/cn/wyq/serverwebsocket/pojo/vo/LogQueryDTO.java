package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

/**
 * LogQueryDTO：视图对象，用于组织接口返回和展示数据。
 */
@Data
public class LogQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 15;
    private Integer userId;
    private Integer type;
    private String startDate;
    private String endDate;
}