package cn.wyq.serverwebsocket.pojo.entity;

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

import java.time.LocalDateTime;

/**
 * Message：业务实体对象，用于映射和传递持久化数据。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    @ExcelProperty("消息编号")
//    @ToolParam(required = false,description = "消息编号")
    private Integer id;
    @ExcelProperty("消息内容")
//    @ToolParam(required = false,description = "消息内容=空：已撤回消息，其他：普通消息")
    private String message;
    @ExcelProperty("发送人")
//    @ToolParam(required = false,description = "发送人")
    private String fromName;
    @ExcelProperty("接收人")
//    @ToolParam(required = false,description = "接收人=空：所有用户，其他：指定用户")
    private String toName;
    //规定时间显示类型
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("发送时间")
    @ColumnWidth(25)
//    @ToolParam(required = false,description = "发送时间")
    private LocalDateTime sendAt;
}
