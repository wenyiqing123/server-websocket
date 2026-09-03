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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("u_transfer")
public class Transfer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String transferNo;
    private Integer senderId;
    private Integer receiverId;
    private BigDecimal amount;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime acceptTime;
    private LocalDateTime returnTime;
}
