/*
 Navicat Premium Data Transfer

 Source Server         : mvc1
 Source Server Type    : MySQL
 Source Server Version : 80033 (8.0.33)
 Source Host           : localhost:3306
 Source Schema         : chat

 Target Server Type    : MySQL
 Target Server Version : 80033 (8.0.33)
 File Encoding         : 65001

 Date: 06/09/2026 18:57:49
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_agent
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent`;
CREATE TABLE `ai_agent`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鏅鸿兘浣撶紪鍙',
  `owner_user_id` int NOT NULL COMMENT '鎵?睘鐢ㄦ埛缂栧彿',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鏅鸿兘浣撳悕绉',
  `avatar_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶村儚鍦板潃',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '绯荤粺鎻愮ず璇',
  `welcome_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '娆㈣繋璇',
  `model` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'qwen3.8-max-0902' COMMENT '模型名称',
  `temperature` decimal(3, 2) NOT NULL DEFAULT 0.70 COMMENT '娓╁害鍙傛暟',
  `max_tokens` int NOT NULL DEFAULT 2048 COMMENT '鏈?ぇ杈撳嚭 token 鏁',
  `visibility` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'private' COMMENT '鍙??鑼冨洿',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '鐘舵?锛?绂佺敤锛?鍚?敤',
  `is_builtin` tinyint NOT NULL DEFAULT 0 COMMENT '鏄?惁鍐呯疆鏅鸿兘浣',
  `prompt_version` int NOT NULL DEFAULT 1 COMMENT '鎻愮ず璇嶇増鏈',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '杞?垹闄ゆ爣璁',
  `use_count` bigint NOT NULL DEFAULT 0 COMMENT '浣跨敤娆℃暟',
  `last_used_at` datetime NULL DEFAULT NULL COMMENT '鏈?繎浣跨敤鏃堕棿',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_agent_owner`(`owner_user_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_ai_agent_status`(`status` ASC, `deleted` ASC) USING BTREE,
  CONSTRAINT `fk_ai_agent_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐢ㄦ埛鑷?畾涔堿I鏅鸿兘浣' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_chat_memory
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_memory`;
CREATE TABLE `ai_chat_memory`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `timestamp` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_spring_ai_chat_memory_conversation_id`(`conversation_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5486 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_chat_session
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `owner_user_id` int NULL DEFAULT NULL COMMENT '不可变所属用户编号',
  `user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `last_message_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `agent_id` bigint NULL DEFAULT NULL COMMENT '鑷?畾涔夋櫤鑳戒綋缂栧彿',
  `prompt_version` int NULL DEFAULT NULL COMMENT '鍒涘缓浼氳瘽鏃剁殑鎻愮ず璇嶇増鏈',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_chat_session_agent`(`agent_id` ASC) USING BTREE,
  INDEX `idx_ai_chat_session_owner`(`owner_user_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_service_question
-- ----------------------------
DROP TABLE IF EXISTS `ai_service_question`;
CREATE TABLE `ai_service_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '题库记录编号',
  `question_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '归一化问题 SHA-256 哈希',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户原始问题',
  `normalized_question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用于相似度匹配的归一化问题',
  `source_type` varchar(32) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL DEFAULT 'service' COMMENT '问题来源类型',
  `use_count` int NOT NULL DEFAULT 1 COMMENT '问题累计使用次数',
  `last_user_id` int NULL DEFAULT NULL COMMENT '最近一次提交问题的用户编号',
  `last_used_at` datetime NULL DEFAULT NULL COMMENT '最近一次使用时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_service_question_source_hash`(`source_type` ASC, `question_hash` ASC) USING BTREE,
  INDEX `idx_ai_service_question_source_updated`(`source_type` ASC, `updated_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能客服问题提示题库' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_session_document
-- ----------------------------
DROP TABLE IF EXISTS `ai_session_document`;
CREATE TABLE `ai_session_document`  (
  `session_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `document_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`session_id`, `document_id`) USING BTREE,
  INDEX `idx_session_document_document_id`(`document_id` ASC) USING BTREE,
  CONSTRAINT `fk_session_document_document` FOREIGN KEY (`document_id`) REFERENCES `sys_document` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_session_document_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chat_group
-- ----------------------------
DROP TABLE IF EXISTS `chat_group`;
CREATE TABLE `chat_group`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '群组ID (由后端生成 UUID)',
  `group_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '群名称',
  `owner_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '群主用户名',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '群头像图片路径',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群组基本信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for conversation
-- ----------------------------
DROP TABLE IF EXISTS `conversation`;
CREATE TABLE `conversation`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '对话id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '对话名',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '对话用户名',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 159 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'ai功能的对话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for conversation_message
-- ----------------------------
DROP TABLE IF EXISTS `conversation_message`;
CREATE TABLE `conversation_message`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '消息id',
  `conversation_id` int NOT NULL COMMENT '消息所属对话id',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `role` int NOT NULL COMMENT '消息所属角色（1：用户，2：系统）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 778 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'ai功能对话消息记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for friend
-- ----------------------------
DROP TABLE IF EXISTS `friend`;
CREATE TABLE `friend`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '关系记录ID',
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发起申请的用户名',
  `friend_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '被申请的好友用户名',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态: 0-待通过, 1-已是好友, 2-已拒绝',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_friend`(`user_name` ASC, `friend_name` ASC) USING BTREE COMMENT '防止重复发送申请'
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '好友关系与申请表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for group_member
-- ----------------------------
DROP TABLE IF EXISTS `group_member`;
CREATE TABLE `group_member`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联的群组ID',
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '群成员用户名',
  `role` int NOT NULL DEFAULT 2 COMMENT '角色: 0-群主, 1-管理员, 2-普通成员',
  `join_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_group_user`(`group_id` ASC, `user_name` ASC) USING BTREE COMMENT '防止同一个人重复进群'
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群成员关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '消息编号',
  `message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '消息内容',
  `from_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发送者',
  `to_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '接收者',
  `send_at` datetime NOT NULL COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10963 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天功能消息记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for message_intention
-- ----------------------------
DROP TABLE IF EXISTS `message_intention`;
CREATE TABLE `message_intention`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提问用户',
  `tag_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标准化意图标签(如: Redis锁)',
  `original_prompt` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户的真实提问原话(用于兜底和展示)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tag`(`tag_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息意图表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for recharge_order
-- ----------------------------
DROP TABLE IF EXISTS `recharge_order`;
CREATE TABLE `recharge_order`  (
  `out_trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '系统内部订单号(传给支付宝的单号)',
  `user_id` int NOT NULL COMMENT '充值用户ID',
  `alipay_trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付宝官方流水号',
  `amount` decimal(10, 2) NOT NULL COMMENT '充值金额',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付, 1-支付成功, 2-已关闭',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付成功时间',
  PRIMARY KEY (`out_trade_no`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '充值订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键（代码里调用的唯一标识）',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置值（用 text 以便存储较长的 AI 提示词或 JSON）',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置名称（给后台管理员看的）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注说明',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE COMMENT '保证配置键不重复，加快查询速度'
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统全局动态配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_document
-- ----------------------------
DROP TABLE IF EXISTS `sys_document`;
CREATE TABLE `sys_document`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文档ID (UUID)',
  `owner_user_id` int NULL DEFAULT NULL COMMENT '上传用户编号',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '本地服务器存储路径',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小(字节)',
  `content_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL COMMENT '原始文件内容 SHA-256',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_document_owner_hash`(`owner_user_id` ASC, `content_hash` ASC) USING BTREE,
  INDEX `idx_sys_document_owner`(`owner_user_id` ASC, `create_time` ASC) USING BTREE,
  CONSTRAINT `fk_sys_document_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能分析文档表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for test
-- ----------------------------
DROP TABLE IF EXISTS `test`;
CREATE TABLE `test`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `create_user` bigint NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `update_user` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 93 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '资金变动流水表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '红包主表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 2095091257937940483 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '红包领取记录明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for u_transfer
-- ----------------------------
DROP TABLE IF EXISTS `u_transfer`;
CREATE TABLE `u_transfer`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transfer_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sender_id` int NOT NULL,
  `receiver_id` int NOT NULL,
  `amount` decimal(18, 2) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `accept_time` datetime NULL DEFAULT NULL,
  `return_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_transfer_no`(`transfer_no` ASC) USING BTREE,
  INDEX `idx_transfer_sender`(`sender_id` ASC) USING BTREE,
  INDEX `idx_transfer_receiver_status`(`receiver_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_transfer_expire`(`status` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户转账记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户编号',
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '头像图片路径',
  `role` int NOT NULL COMMENT '用户权限（0：超级管理员，1：管理员，2：用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '绑定邮箱',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态(0:待审批, 1:正常, 2:拒绝)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_name`(`user_name` ASC) USING BTREE,
  UNIQUE INDEX `email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
