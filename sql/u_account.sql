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

 Date: 11/05/2026 20:57:51
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for u_account
-- ----------------------------
DROP TABLE IF EXISTS `u_account`;
CREATE TABLE `u_account`  (
  `user_id` int NOT NULL COMMENT '用户ID，关联用户表',
  `balance` decimal(18, 2) NOT NULL DEFAULT 0.00 COMMENT '当前余额',
  `freeze_amount` decimal(18, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额（处理中或提现中）',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户账户余额表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of u_account
-- ----------------------------
INSERT INTO `u_account` VALUES (60, 72.38, 0.00, '2026-04-29 22:48:59');
INSERT INTO `u_account` VALUES (62, 2.67, 0.00, '2026-04-29 22:49:20');
INSERT INTO `u_account` VALUES (63, 13.27, 0.00, '2026-04-29 22:49:28');

SET FOREIGN_KEY_CHECKS = 1;
