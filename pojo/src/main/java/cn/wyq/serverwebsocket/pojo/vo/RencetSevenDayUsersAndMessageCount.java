package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * RencetSevenDayUsersAndMessageCount：视图对象，用于组织接口返回和展示数据。
 */
@Data
public class RencetSevenDayUsersAndMessageCount {
    private LocalDate sendAt;
    private String userName;
    private int messageCount;

}
