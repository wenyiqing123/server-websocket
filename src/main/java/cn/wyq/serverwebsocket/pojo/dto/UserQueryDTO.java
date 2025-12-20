package cn.wyq.serverwebsocket.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class UserQueryDTO {
    private int page = 1;

    private int pageSize = 10;
    private Integer id;
    private String userName;
    private Integer role;

}
