package cn.wyq.serverwebsocket.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class MessageQueryDTO {


    private int page = 1;

    private int pageSize = 10;

    private Integer id;

    private String message;

    private String fromName;

    private String toName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 用于接收参数（请求参数）
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") // 用于序列化和反序列化 JSON
    private LocalDateTime sendAtStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sendAtEnd;

}
