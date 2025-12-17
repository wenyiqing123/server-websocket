package cn.wyq.serverwebsocket.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class Test {
    private Integer id;
    private String name;

    private String say(String name) {
        return name;
    }

}
