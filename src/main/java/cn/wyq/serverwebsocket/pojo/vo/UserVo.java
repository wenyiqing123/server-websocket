package cn.wyq.serverwebsocket.pojo.vo;

import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class UserVo {
    private String token;
    private UserEntity user;
}
