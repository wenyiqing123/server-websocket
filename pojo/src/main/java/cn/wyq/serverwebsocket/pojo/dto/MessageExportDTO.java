package cn.wyq.serverwebsocket.pojo.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageExportDTO {

    @ExcelProperty("消息编号")
    @ColumnWidth(15)
    private Integer id;

    @ExcelProperty("消息内容")
    @ColumnWidth(40)
    private String message;

    @ExcelProperty("发送者")
    @ColumnWidth(20)
    private String fromName;

    @ExcelProperty("接收者")
    @ColumnWidth(25) // 群聊名称可能会比较长，稍微给宽一点
    private String toName;

    @ExcelProperty("发送时间")
    @ColumnWidth(25)
    private LocalDateTime sendAt;
}