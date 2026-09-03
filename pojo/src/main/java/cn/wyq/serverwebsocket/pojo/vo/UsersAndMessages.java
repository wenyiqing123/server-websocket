package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * UsersAndMessages：视图对象，用于组织接口返回和展示数据。
 */
@Data
public class UsersAndMessages {
    private LocalDate sendAt;
    private int userCount;
    private int messageCount;

}
