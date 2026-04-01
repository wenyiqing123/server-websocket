package cn.wyq.serverwebsocket.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_document")
public class SysDocument {
    @TableId
    private String id;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private Date createTime;
}