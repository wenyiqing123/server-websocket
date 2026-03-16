package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RencetSevenDayUsersAndMessageCount {
    private LocalDate sendAt;
    private String userName;
    private int messageCount;

}
