/*
 Navicat Premium Data Transfer

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80033 (8.0.33)
 Source Host           : localhost:3306
 Source Schema         : chat

 Target Server Type    : MySQL
 Target Server Version : 80033 (8.0.33)
 File Encoding         : 65001

 Date: 11/05/2026 20:57:55
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for u_account_log
-- ----------------------------
DROP TABLE IF EXISTS `u_account_log`;
CREATE TABLE `u_account_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '所属用户ID',
  `type` tinyint NOT NULL COMMENT '变动类型：1-充值、2-发红包、3-抢红包、4-AI算力、5-人工调账、6-过期退回、7-转账',
  `amount` decimal(18, 2) NOT NULL COMMENT '变动金额（正数为进账，负数为出账）',
  `balance_after` decimal(18, 2) NOT NULL COMMENT '变动后的余额（用于后期对账）',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联的业务单号（转账单号或红包单号）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_order_no`(`order_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 52 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '资金变动流水表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of u_account_log
-- ----------------------------
INSERT INTO `u_account_log` VALUES (35, 60, 2, -10.00, 0.00, 'RP17774741499270fdd22', '发出群聊红包', '2026-04-29 22:49:10');
INSERT INTO `u_account_log` VALUES (36, 60, 3, 5.06, 5.06, 'GRAB177747415156378b13c', '抢到群聊红包', '2026-04-29 22:49:12');
INSERT INTO `u_account_log` VALUES (37, 62, 3, 1.67, 1.67, 'GRAB1777474160214af8653', '抢到群聊红包', '2026-04-29 22:49:20');
INSERT INTO `u_account_log` VALUES (38, 63, 3, 3.27, 3.27, 'GRAB1777474168341fb64fd', '抢到群聊红包', '2026-04-29 22:49:28');
INSERT INTO `u_account_log` VALUES (39, 63, 1, 10.00, 13.27, 'PAY1777475125805c1932a', '系统模拟充值', '2026-04-29 23:05:26');
INSERT INTO `u_account_log` VALUES (40, 60, 2, -1.00, 4.06, 'RP1777603152815b7dc1e', '发出群聊红包', '2026-05-01 10:39:13');
INSERT INTO `u_account_log` VALUES (41, 60, 2, -1.00, 3.06, 'RP1777603383291fa5cf1', '发出群聊红包', '2026-05-01 10:43:03');
INSERT INTO `u_account_log` VALUES (42, 60, 2, -1.00, 2.06, 'RP1777603885160f64a20', '发出专属红包', '2026-05-01 10:51:25');
INSERT INTO `u_account_log` VALUES (43, 62, 3, 1.00, 2.67, 'GRAB177760389540738bd38', '抢到群聊红包', '2026-05-01 10:51:35');
INSERT INTO `u_account_log` VALUES (44, 60, 3, 0.32, 2.38, 'GRAB1777603946668f3f262', '抢到群聊红包', '2026-05-01 10:52:27');
INSERT INTO `u_account_log` VALUES (45, 60, 1, 10.00, 12.38, 'PAY17776074284284cffb7', '系统模拟充值', '2026-05-01 11:50:28');
INSERT INTO `u_account_log` VALUES (46, 60, 1, 10.00, 22.38, 'RCG1777608905764097ebf', '支付宝沙盒充值', '2026-05-01 12:15:06');
INSERT INTO `u_account_log` VALUES (47, 60, 1, 10.00, 32.38, 'RCG17776091356006b2605', '支付宝沙盒充值', '2026-05-01 12:18:56');
INSERT INTO `u_account_log` VALUES (48, 60, 1, 10.00, 42.38, 'RCG1777609797052c9f0fa', '支付宝沙盒充值', '2026-05-01 12:29:57');
INSERT INTO `u_account_log` VALUES (49, 60, 1, 10.00, 52.38, 'RCG17776101457752e84a5', '支付宝沙盒充值', '2026-05-01 12:35:46');
INSERT INTO `u_account_log` VALUES (50, 60, 1, 10.00, 62.38, 'RCG17776133453820c76dc', '支付宝沙盒充值', '2026-05-01 13:29:05');
INSERT INTO `u_account_log` VALUES (51, 60, 1, 10.00, 72.38, 'RCG1778211263473b631ef', '支付宝沙盒充值', '2026-05-08 11:34:23');

SET FOREIGN_KEY_CHECKS = 1;
