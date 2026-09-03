package cn.wyq.serverwebsocket.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserQueryExportDTO：数据传输对象，用于承载接口请求参数。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserQueryExportDTO {
    private Integer id;
    private String userName;
    private Integer role;
    private String email;

}
