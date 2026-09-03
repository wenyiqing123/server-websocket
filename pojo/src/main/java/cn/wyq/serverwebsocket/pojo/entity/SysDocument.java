package cn.wyq.serverwebsocket.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * SysDocument：业务实体对象，用于映射和传递持久化数据。
 */
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