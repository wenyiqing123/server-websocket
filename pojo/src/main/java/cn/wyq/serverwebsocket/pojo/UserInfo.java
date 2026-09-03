package cn.wyq.serverwebsocket.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * UserInfo：后端领域对象，封装相关数据和处理能力。
 */
@Data
@AllArgsConstructor
public class UserInfo {
    private int status;
    private String path;
}
