package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

@Data
public class LogQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 15;
    private Integer userId;
    private Integer type;
    private String startDate;
    private String endDate;
}