package cn.wyq.serverwebsocket.pojo.entity;

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
@TableName("u_red_packet_record")
public class RedPacketRecord {
    private Long id;
    private Long redPacketId;
    private Integer userId;
    private BigDecimal amount;
    private LocalDateTime createTime;
}