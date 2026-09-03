SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `u_transfer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transfer_no` varchar(64) NOT NULL COMMENT '转账业务单号',
  `sender_id` int NOT NULL COMMENT '付款用户ID',
  `receiver_id` int NOT NULL COMMENT '收款用户ID',
  `amount` decimal(18,2) NOT NULL COMMENT '转账金额',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-待接收、1-已接收、2-已退回、3-已拒绝',
  `remark` varchar(255) DEFAULT NULL COMMENT '转账备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `accept_time` datetime DEFAULT NULL,
  `return_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transfer_no` (`transfer_no`),
  KEY `idx_transfer_sender` (`sender_id`),
  KEY `idx_transfer_receiver_status` (`receiver_id`, `status`),
  KEY `idx_transfer_expire` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户转账记录';
