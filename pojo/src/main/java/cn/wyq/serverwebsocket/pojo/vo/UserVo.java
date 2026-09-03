package cn.wyq.serverwebsocket.pojo.vo;


import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserVo：视图对象，用于组织接口返回和展示数据。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor

public class UserVo {
    private String accessToken;
    private String refreshToken;
    private UserEntity user;
    private String email;
}
