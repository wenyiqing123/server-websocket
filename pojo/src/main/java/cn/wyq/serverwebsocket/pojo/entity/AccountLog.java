package cn.wyq.serverwebsocket.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("u_account_log")
public class AccountLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer userId;       // 改为 Integer
    /**
     * 变动类型：1-转账、2-发红包、3-抢红包
     */
    private Integer type;

    /**
     * 变动金额（正数为进账，负数为出账）
     */
    private BigDecimal amount;

    /**
     * 变动后的余额
     */
    private BigDecimal balanceAfter;

    /**
     * 业务单号（UUID或雪花算法生成的全局唯一标识）
     */
    private String orderNo;

    private String remark;

    private LocalDateTime createTime;
}