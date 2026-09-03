package cn.wyq.serverwebsocket.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserUpdatePasswordDTO：数据传输对象，用于承载接口请求参数。
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserUpdatePasswordDTO {
    private String oldPassword;
    private String newPassword;
}
