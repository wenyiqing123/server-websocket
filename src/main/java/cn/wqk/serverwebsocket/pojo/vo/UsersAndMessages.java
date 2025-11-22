package cn.wqk.serverwebsocket.pojo.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UsersAndMessages {
    private LocalDate sendAt;
    private int userCount;
    private int messageCount;

}
