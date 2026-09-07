package cn.wyq.serverwebsocket.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
    /** 上传该文档的用户编号；无法确认归属的历史数据保留为空且不对普通用户开放。 */
    @TableField("owner_user_id")
    private Integer ownerUserId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    /** 原始文件内容的 SHA-256 摘要；同一用户不能保存相同内容的文档。 */
    @TableField("content_hash")
    private String contentHash;
    private Date createTime;
}
