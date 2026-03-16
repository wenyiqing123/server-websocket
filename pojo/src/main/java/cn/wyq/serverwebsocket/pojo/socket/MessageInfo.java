package cn.wyq.serverwebsocket.pojo.socket;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MessageInfo {
    private Integer id;
    private String message;
    private String fromName;
    private String toName;
    private LocalDateTime time;
//    private int recalled;

}
