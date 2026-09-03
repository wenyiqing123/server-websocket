package cn.wyq.serverwebsocket.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RedPacket：业务实体对象，用于映射和传递持久化数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("u_red_packet")
public class RedPacket {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发红包者ID
     */
    private Integer senderId;     // 改为 Integer
    /**
     * 房间/群组ID
     */
    private String groupId;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 总个数
     */
    private Integer totalCount;

    /**
     * 剩余金额
     */
    private BigDecimal remainingAmount;

    /**
     * 剩余个数
     */
    private Integer remainingCount;

    /**
     * 状态：0-进行中、1-已抢完、2-已过期退回
     */
    private Integer status;

    private Integer receiverId;   // 改为 Integer
    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}
