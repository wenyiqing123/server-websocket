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

 Date: 11/05/2026 20:58:00
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for u_red_packet
-- ----------------------------
DROP TABLE IF EXISTS `u_red_packet`;
CREATE TABLE `u_red_packet`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint NOT NULL COMMENT '发红包的用户ID',
  `group_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联的群聊或房间ID',
  `total_amount` decimal(18, 2) NOT NULL COMMENT '红包总金额',
  `total_count` int NOT NULL COMMENT '红包总个数',
  `remaining_amount` decimal(18, 2) NOT NULL COMMENT '剩余金额',
  `remaining_count` int NOT NULL COMMENT '剩余个数',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '红包状态：0-进行中、1-已抢完、2-已退回（过期后）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finish_time` datetime NULL DEFAULT NULL COMMENT '抢完的时间',
  `receiver_id` bigint NULL DEFAULT NULL COMMENT '指定接收人的 ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sender_id`(`sender_id` ASC) USING BTREE,
  INDEX `idx_room_id`(`group_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '红包主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of u_red_packet
-- ----------------------------
INSERT INTO `u_red_packet` VALUES (14, 60, 'GROUP_1776911972861', 10.00, 3, 0.00, 0, 0, '2026-04-29 22:49:10', NULL, NULL);
INSERT INTO `u_red_packet` VALUES (15, 60, 'GROUP_1776911972861', 1.00, 3, 0.68, 2, 0, '2026-05-01 10:39:13', NULL, NULL);
INSERT INTO `u_red_packet` VALUES (16, 60, 'GROUP_1776911972861', 1.00, 3, 1.00, 3, 0, '2026-05-01 10:43:03', NULL, NULL);
INSERT INTO `u_red_packet` VALUES (17, 60, '0', 1.00, 1, 0.00, 0, 0, '2026-05-01 10:51:25', NULL, 62);

SET FOREIGN_KEY_CHECKS = 1;
