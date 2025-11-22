package cn.wqk.serverwebsocket.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInfo {
    private int status;
    private String path;
}
