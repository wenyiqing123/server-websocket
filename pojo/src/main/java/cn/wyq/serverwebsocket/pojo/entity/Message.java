package cn.wyq.serverwebsocket.pojo.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    @ExcelProperty("消息编号")
    private Integer id;
    @ExcelProperty("消息内容")
    private String message;
    @ExcelProperty("发送人")
    private String fromName;
    @ExcelProperty("接收人")
    private String toName;
    //规定时间显示类型
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("发送时间")
    @ColumnWidth(25)
    private LocalDateTime sendAt;


}
