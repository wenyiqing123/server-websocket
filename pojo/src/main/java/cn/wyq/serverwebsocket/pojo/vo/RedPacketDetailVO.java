package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RedPacketDetailVO {
    private Long redPacketId;
    private Integer senderId;
    private String senderName;
    private String senderAvatar;
    private String blessing; // 祝福语
    private BigDecimal totalAmount;
    private Integer totalCount;
    private BigDecimal remainingAmount;
    private Integer remainingCount;
    private Integer status; // 0-进行中, 1-已抢完, 2-已退回

    // 当前查看该详情的用户，自己抢到的金额（如果没抢到则为 null）
    private BigDecimal myAmount;

    // 领取记录明细列表
    private List<RecordItem> recordList;

    @Data
    public static class RecordItem {
        private Integer userId;
        private String userName;
        private String avatar;
        private BigDecimal amount;
        private LocalDateTime grabTime;
        private Boolean isBestLuck; // 是否手气最佳
    }
}