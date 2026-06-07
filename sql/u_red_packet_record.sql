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

 Date: 11/05/2026 20:58:03
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for u_red_packet_record
-- ----------------------------
DROP TABLE IF EXISTS `u_red_packet_record`;
CREATE TABLE `u_red_packet_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `red_packet_id` bigint NOT NULL COMMENT '红包主表ID',
  `user_id` int NOT NULL COMMENT '抢红包的用户ID',
  `amount` decimal(10, 2) NOT NULL COMMENT '抢到的金额',
  `create_time` datetime NOT NULL COMMENT '抢到的时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_red_packet_id`(`red_packet_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2050045654289383427 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '红包领取记录明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of u_red_packet_record
-- ----------------------------
INSERT INTO `u_red_packet_record` VALUES (2049501254207520770, 14, 60, 5.06, '2026-04-29 22:49:12');
INSERT INTO `u_red_packet_record` VALUES (2049501290446307329, 14, 62, 1.67, '2026-04-29 22:49:20');
INSERT INTO `u_red_packet_record` VALUES (2049501324541804545, 14, 63, 3.27, '2026-04-29 22:49:28');
INSERT INTO `u_red_packet_record` VALUES (2050045439306137602, 17, 62, 1.00, '2026-05-01 10:51:35');
INSERT INTO `u_red_packet_record` VALUES (2050045654289383426, 15, 60, 0.32, '2026-05-01 10:52:27');

SET FOREIGN_KEY_CHECKS = 1;
