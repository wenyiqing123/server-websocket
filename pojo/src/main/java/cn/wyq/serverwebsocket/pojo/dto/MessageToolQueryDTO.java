package cn.wyq.serverwebsocket.pojo.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;



@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("message")
public class MessageToolQueryDTO {
    @TableId(type = IdType.AUTO)
    @ExcelProperty("消息编号")
    @ToolParam(required = false,description = "消息编号")
    private Integer id;
    @ExcelProperty("消息内容")
    @ToolParam(required = false,description = "消息内容=空：已撤回消息，其他：普通消息")
    private String message;
    @ExcelProperty("发送人")
    @ToolParam(required = false,description = "发送人")
    private String fromName;
    @ExcelProperty("接收人")
    @ToolParam(required = false,description = "接收人=空：所有用户，其他：指定用户")
    private String toName;
    //规定时间显示类型
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("发送时间")
    @ColumnWidth(25)
    @ToolParam(required = false,description = "发送时间")
    private LocalDateTime sendAt;

    // 💡 专门接收前端传来的时间区间
//    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 接收前端格式化时间
    @ToolParam(required = false,description = "发送开始时间=开始时间")
    private LocalDateTime sendAtStart; // 开始时间

//    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToolParam(required = false,description = "发送结束时间=结束时间")
    private LocalDateTime sendAtEnd;   // 结束时间
}