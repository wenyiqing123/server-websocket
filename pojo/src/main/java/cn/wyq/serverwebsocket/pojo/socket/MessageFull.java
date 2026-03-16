package cn.wyq.serverwebsocket.pojo.socket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageFull {
    private Integer id;
    private String message;
    private String fromName;
    private String fromPath;
    private String toName;
    private String toPath;
    private LocalDateTime time;
//    private int recalled;
}
