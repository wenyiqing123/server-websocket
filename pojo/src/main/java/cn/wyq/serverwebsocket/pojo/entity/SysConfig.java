package cn.wyq.serverwebsocket.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SysConfig：业务实体对象，用于映射和传递持久化数据。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("sys_config")
public class SysConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("config_key")
    private String configKey;
    @TableField("config_value")
    private String configValue;
    @TableField("config_name")
    private String configName;
    @TableField("remark")
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
