package cn.wyq.serverwebsocket.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TestDTO：数据传输对象，用于承载接口请求参数。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TestDTO {
    private Long id;
    private String username;
}
