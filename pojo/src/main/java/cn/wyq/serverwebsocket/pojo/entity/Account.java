package cn.wyq.serverwebsocket.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Account：业务实体对象，用于映射和传递持久化数据。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("u_account")
public class Account {

    @TableId
    private Integer userId;       // 改为 Integer
    /**
     * 当前可用余额
     */
    private BigDecimal balance;

    /**
     * 冻结金额
     */
    private BigDecimal freezeAmount;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}