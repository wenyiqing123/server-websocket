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

 Date: 19/09/2026 16:40:18
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_agent
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent`;
CREATE TABLE `ai_agent`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '智能体编号',
  `owner_user_id` int NOT NULL COMMENT '所属用户编号',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '智能体名称',
  `avatar_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像地址',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '系统提示词',
  `welcome_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '欢迎语',
  `model` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'qwen3.8-max-0902' COMMENT '模型名称',
  `temperature` decimal(3, 2) NOT NULL DEFAULT 0.70 COMMENT '温度参数',
  `max_tokens` int NOT NULL DEFAULT 2048 COMMENT '最大输出 token 数',
  `visibility` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'private' COMMENT '可见范围（private 私有、public 公开）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态（0 禁用、1 启用）',
  `is_builtin` tinyint NOT NULL DEFAULT 0 COMMENT '是否为内置智能体（0 否、1 是）',
  `prompt_version` int NOT NULL DEFAULT 1 COMMENT '提示词版本',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记（0 未删除、1 已删除）',
  `use_count` bigint NOT NULL DEFAULT 0 COMMENT '使用次数',
  `last_used_at` datetime NULL DEFAULT NULL COMMENT '最近使用时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_agent_owner`(`owner_user_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_ai_agent_status`(`status` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_ai_agent_deleted_updated`(`deleted` ASC, `updated_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_ai_agent_owner_deleted_updated`(`owner_user_id` ASC, `deleted` ASC, `updated_at` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_ai_agent_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户自定义 AI 智能体表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_chat_memory
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_memory`;
CREATE TABLE `ai_chat_memory`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记忆消息编号',
  `conversation_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话标识',
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息类型（user 用户、assistant 助手、system 系统）',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `timestamp` datetime NOT NULL COMMENT '消息时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_spring_ai_chat_memory_conversation_id`(`conversation_id` ASC) USING BTREE,
  INDEX `idx_ai_chat_memory_conversation_time`(`conversation_id` ASC, `timestamp` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5750 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 对话记忆消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_chat_session
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话编号',
  `owner_user_id` int NULL DEFAULT NULL COMMENT '不可变所属用户编号',
  `user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话用户标识',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话标题',
  `last_message_at` datetime NULL DEFAULT NULL COMMENT '最近消息时间',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `agent_id` bigint NULL DEFAULT NULL COMMENT '自定义智能体编号',
  `prompt_version` int NULL DEFAULT NULL COMMENT '创建会话时的提示词版本',
  `ticket_status` varchar(20) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL DEFAULT 'OPEN' COMMENT '工单状态（OPEN 开启、PROCESSING 处理中、CLOSED 已关闭）',
  `admin_remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客服管理员备注',
  `closed_at` datetime NULL DEFAULT NULL COMMENT '工单关闭时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_chat_session_agent`(`agent_id` ASC) USING BTREE,
  INDEX `idx_ai_chat_session_owner`(`owner_user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_ai_chat_session_agent_owner`(`agent_id` ASC, `owner_user_id` ASC) USING BTREE,
  INDEX `idx_ai_chat_session_ticket_time`(`ticket_status` ASC, `last_message_at` ASC, `created_at` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 对话会话及客服工单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `ai_knowledge_base`;
CREATE TABLE `ai_knowledge_base`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识库编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库说明',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1启用，0停用',
  `source_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'UPLOAD' COMMENT '知识库来源类型（LEGACY 历史文件、UPLOAD 上传文件）',
  `current_version` int NOT NULL DEFAULT 0 COMMENT '最近发布版本',
  `document_count` int NOT NULL DEFAULT 0 COMMENT '已发布文档数',
  `chunk_count` int NOT NULL DEFAULT 0 COMMENT '已发布向量切片数',
  `created_by` int NULL DEFAULT NULL COMMENT '创建管理员编号',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '软删除标记',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_knowledge_base_name`(`name` ASC) USING BTREE,
  INDEX `idx_ai_knowledge_base_status`(`status` ASC, `deleted` ASC) USING BTREE,
  INDEX `fk_ai_knowledge_base_creator`(`created_by` ASC) USING BTREE,
  CONSTRAINT `fk_ai_knowledge_base_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能客服公共知识库' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_knowledge_chunk_hash
-- ----------------------------
DROP TABLE IF EXISTS `ai_knowledge_chunk_hash`;
CREATE TABLE `ai_knowledge_chunk_hash`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '切片去重记录编号',
  `knowledge_base_id` bigint NOT NULL COMMENT '知识库编号',
  `document_id` bigint NOT NULL COMMENT '文档编号',
  `document_version` int NOT NULL COMMENT '文档发布版本',
  `chunk_index` int NOT NULL COMMENT '文档内切片序号',
  `content_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '规范化切片SHA-256',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_knowledge_chunk_hash`(`knowledge_base_id` ASC, `content_hash` ASC) USING BTREE,
  INDEX `idx_ai_knowledge_chunk_document`(`document_id` ASC, `document_version` ASC) USING BTREE,
  CONSTRAINT `fk_ai_knowledge_chunk_base` FOREIGN KEY (`knowledge_base_id`) REFERENCES `ai_knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_knowledge_chunk_document` FOREIGN KEY (`document_id`) REFERENCES `ai_knowledge_document` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库切片去重索引' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_knowledge_document
-- ----------------------------
DROP TABLE IF EXISTS `ai_knowledge_document`;
CREATE TABLE `ai_knowledge_document`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文档编号',
  `document_uuid` char(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '私有随机存储标识',
  `knowledge_base_id` bigint NOT NULL COMMENT '所属知识库',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '展示文件名',
  `storage_key` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '私有目录中的随机文件名',
  `file_size` bigint NOT NULL DEFAULT 0 COMMENT '原文件字节数',
  `content_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '文件SHA-256',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '软删除标记',
  `active_content_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin GENERATED ALWAYS AS (if((`deleted` = 0),`content_hash`,NULL)) STORED COMMENT '仅未删除文档参与唯一校验' NULL,
  `file_type` varchar(10) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '文档文件类型（PDF、TXT、MD）',
  `process_status` varchar(20) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL DEFAULT 'PARSING' COMMENT '文档处理状态（PARSING 解析中、VECTORIZING 向量化中、PUBLISHED 已发布、FAILED 失败、DELETED 已删除）',
  `current_version` int NOT NULL DEFAULT 0 COMMENT '当前成功发布版本',
  `chunk_count` int NOT NULL DEFAULT 0 COMMENT '当前版本切片数',
  `duplicate_count` int NOT NULL DEFAULT 0 COMMENT '索引时排除的重复片段数',
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最近处理错误',
  `created_by` int NULL DEFAULT NULL COMMENT '上传管理员编号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_knowledge_document_uuid`(`document_uuid` ASC) USING BTREE,
  UNIQUE INDEX `uk_ai_knowledge_document_storage`(`storage_key` ASC) USING BTREE,
  UNIQUE INDEX `uk_ai_knowledge_document_hash`(`knowledge_base_id` ASC, `active_content_hash` ASC) USING BTREE,
  INDEX `idx_ai_knowledge_document_status`(`knowledge_base_id` ASC, `process_status` ASC, `deleted` ASC) USING BTREE,
  INDEX `fk_ai_knowledge_document_creator`(`created_by` ASC) USING BTREE,
  INDEX `idx_ai_knowledge_document_base_deleted_id`(`knowledge_base_id` ASC, `deleted` ASC, `id` ASC) USING BTREE,
  INDEX `idx_ai_knowledge_document_base_hash_deleted`(`knowledge_base_id` ASC, `content_hash` ASC, `deleted` ASC) USING BTREE,
  CONSTRAINT `fk_ai_knowledge_document_base` FOREIGN KEY (`knowledge_base_id`) REFERENCES `ai_knowledge_base` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_knowledge_document_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能客服知识库文档及索引状态' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_service_feedback
-- ----------------------------
DROP TABLE IF EXISTS `ai_service_feedback`;
CREATE TABLE `ai_service_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '反馈记录编号',
  `session_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客服工单编号',
  `message_id` bigint NULL DEFAULT NULL COMMENT 'ai_chat_memory消息编号，实时回答可能为空',
  `message_key` bigint GENERATED ALWAYS AS (coalesce(`message_id`,0)) STORED COMMENT '用于保证实时回答反馈唯一' NULL,
  `user_id` int NOT NULL COMMENT '反馈用户',
  `helpful` tinyint NOT NULL COMMENT '1有用，0无用',
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '无用原因或补充说明',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_service_feedback_message`(`session_id` ASC, `message_key` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_ai_service_feedback_quality`(`helpful` ASC, `updated_at` ASC) USING BTREE,
  INDEX `fk_ai_service_feedback_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_ai_service_feedback_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_service_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能客服回答反馈' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_service_knowledge_binding
-- ----------------------------
DROP TABLE IF EXISTS `ai_service_knowledge_binding`;
CREATE TABLE `ai_service_knowledge_binding`  (
  `knowledge_base_id` bigint NOT NULL COMMENT '知识库编号',
  `mounted` tinyint NOT NULL DEFAULT 1 COMMENT '是否挂载到官方客服',
  `priority` int NOT NULL DEFAULT 0 COMMENT '检索优先级',
  `updated_by` int NULL DEFAULT NULL COMMENT '最近操作管理员',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`knowledge_base_id`) USING BTREE,
  INDEX `fk_ai_service_binding_operator`(`updated_by` ASC) USING BTREE,
  INDEX `idx_ai_service_binding_mounted_priority`(`mounted` ASC, `priority` ASC, `knowledge_base_id` ASC) USING BTREE,
  CONSTRAINT `fk_ai_service_binding_base` FOREIGN KEY (`knowledge_base_id`) REFERENCES `ai_knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_service_binding_operator` FOREIGN KEY (`updated_by`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '官方客服知识库挂载关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_service_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_service_operation_log`;
CREATE TABLE `ai_service_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '操作日志编号',
  `operator_id` int NULL DEFAULT NULL COMMENT '操作管理员',
  `action` varchar(80) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '动作编码',
  `target_type` varchar(40) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '对象类型',
  `target_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '对象编号',
  `change_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '变更摘要，不记录完整系统提示词',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_service_operation_log_time`(`created_at` ASC) USING BTREE,
  INDEX `idx_ai_service_operation_log_target`(`target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `fk_ai_service_operation_log_user`(`operator_id` ASC) USING BTREE,
  CONSTRAINT `fk_ai_service_operation_log_user` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能客服后台操作审计' ROW_FORMAT = Dynamic;

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
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否参与输入联想',
  `review_status` varchar(20) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态（PENDING 待审核、APPROVED 已通过、REJECTED 已拒绝）',
  `admin_remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '管理员备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_service_question_source_hash`(`source_type` ASC, `question_hash` ASC) USING BTREE,
  INDEX `idx_ai_service_question_source_updated`(`source_type` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_ai_service_question_warmup`(`source_type` ASC, `enabled` ASC, `review_status` ASC, `use_count` ASC, `updated_at` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能客服问题提示题库' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_service_question_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_service_question_log`;
CREATE TABLE `ai_service_question_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '问题匹配日志编号',
  `user_id` int NULL DEFAULT NULL COMMENT '提问用户',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实际发送的问题',
  `matched_question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '发送前匹配到的题库问题',
  `similarity` decimal(6, 5) NULL DEFAULT NULL COMMENT '匹配相似度',
  `matched` tinyint NOT NULL DEFAULT 0 COMMENT '是否匹配到提示问题',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_service_question_log_match`(`matched` ASC, `created_at` ASC) USING BTREE,
  INDEX `fk_ai_service_question_log_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_ai_service_question_log_matched_id`(`matched` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_ai_service_question_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服实际问题匹配日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_session_document
-- ----------------------------
DROP TABLE IF EXISTS `ai_session_document`;
CREATE TABLE `ai_session_document`  (
  `session_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工单会话编号',
  `document_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文档编号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
  PRIMARY KEY (`session_id`, `document_id`) USING BTREE,
  INDEX `idx_session_document_document_id`(`document_id` ASC) USING BTREE,
  CONSTRAINT `fk_session_document_document` FOREIGN KEY (`document_id`) REFERENCES `sys_document` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_session_document_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 工单与文档关联表' ROW_FORMAT = DYNAMIC;

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
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_chat_group_owner_id`(`owner_name` ASC, `id` ASC) USING BTREE,
  INDEX `idx_chat_group_create_id`(`create_time` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群组基本信息表' ROW_FORMAT = DYNAMIC;

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
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_user_update`(`user_name` ASC, `update_time` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 159 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'ai功能的对话表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for conversation_message
-- ----------------------------
DROP TABLE IF EXISTS `conversation_message`;
CREATE TABLE `conversation_message`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '消息id',
  `conversation_id` int NOT NULL COMMENT '消息所属对话id',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `role` int NOT NULL COMMENT '消息所属角色（1：用户，2：系统）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_message_conversation_id`(`conversation_id` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 778 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'ai功能对话消息记录表' ROW_FORMAT = DYNAMIC;

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
  UNIQUE INDEX `uk_user_friend`(`user_name` ASC, `friend_name` ASC) USING BTREE COMMENT '防止重复发送申请',
  INDEX `idx_friend_receiver_status_time`(`friend_name` ASC, `status` ASC, `create_time` ASC, `id` ASC) USING BTREE,
  INDEX `idx_friend_owner_status_time`(`user_name` ASC, `status` ASC, `create_time` ASC, `friend_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '好友关系与申请表' ROW_FORMAT = DYNAMIC;

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
  UNIQUE INDEX `uk_group_user`(`group_id` ASC, `user_name` ASC) USING BTREE COMMENT '防止同一个人重复进群',
  INDEX `idx_group_member_user_group`(`user_name` ASC, `group_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群成员关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for login_audit_log
-- ----------------------------
DROP TABLE IF EXISTS `login_audit_log`;
CREATE TABLE `login_audit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志编号',
  `event_id` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'RabbitMQ 事件编号，用于幂等去重',
  `user_id` int NULL DEFAULT NULL COMMENT '用户编号，未知用户的失败登录为空',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户名或请求邮箱',
  `event_type` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '事件类型',
  `success` tinyint NOT NULL DEFAULT 0 COMMENT '是否成功',
  `remote_login` tinyint NOT NULL DEFAULT 0 COMMENT '是否来自此前未记录的 IP',
  `previous_ip` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL COMMENT '此前登录 IP',
  `ip_address` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL COMMENT '本次请求 IP',
  `user_agent` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端 User-Agent',
  `failure_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `occurred_at` datetime NOT NULL COMMENT '事件发生时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志入库时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_login_audit_event_id`(`event_id` ASC) USING BTREE,
  INDEX `idx_login_audit_user_time`(`user_id` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_login_audit_type_time`(`event_type` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_login_audit_success_time`(`success` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_login_audit_occurred_id`(`occurred_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_login_audit_remote_time`(`remote_login` ASC, `occurred_at` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_login_audit_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户登录与认证审计日志' ROW_FORMAT = Dynamic;

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
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_from_to_send`(`from_name` ASC, `to_name` ASC, `send_at` ASC) USING BTREE,
  INDEX `idx_message_to_send_id`(`to_name` ASC, `send_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_message_send_id`(`send_at` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10976 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天功能消息记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for message_intention
-- ----------------------------
DROP TABLE IF EXISTS `message_intention`;
CREATE TABLE `message_intention`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息意图编号',
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提问用户',
  `tag_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标准化意图标签(如: Redis锁)',
  `original_prompt` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户的真实提问原话(用于兜底和展示)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tag`(`tag_name` ASC) USING BTREE,
  INDEX `idx_message_intention_create_id`(`create_time` ASC, `id` ASC) USING BTREE,
  INDEX `idx_message_intention_tag_prompt`(`tag_name` ASC, `original_prompt` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息意图表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for message_log
-- ----------------------------
DROP TABLE IF EXISTS `message_log`;
CREATE TABLE `message_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '本地消息表主键',
  `message_id` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '本地消息编号，也是 RabbitMQ correlation id',
  `message_type` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '消息类型',
  `exchange_name` varchar(128) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT 'RabbitMQ 交换机',
  `routing_key` varchar(128) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT 'RabbitMQ 路由键',
  `payload` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '待发送 JSON 消息体',
  `status` varchar(20) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '消息投递状态（PENDING 待发送、SENDING 发送中、RETRY 待重试、SENT 已发送、FAILED 失败）',
  `attempt_count` int NOT NULL DEFAULT 0 COMMENT '投递尝试次数',
  `next_retry_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下一次允许投递时间',
  `last_attempt_at` datetime NULL DEFAULT NULL COMMENT '最近一次投递时间',
  `confirmed_at` datetime NULL DEFAULT NULL COMMENT 'RabbitMQ publisher confirm 时间',
  `returned_at` datetime NULL DEFAULT NULL COMMENT 'RabbitMQ returned message 时间',
  `last_error` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最近一次失败或返回原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_message_log_message_id`(`message_id` ASC) USING BTREE,
  INDEX `idx_message_log_dispatch`(`status` ASC, `next_retry_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_message_log_stale`(`status` ASC, `last_attempt_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 55 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'RabbitMQ 本地消息表' ROW_FORMAT = DYNAMIC;

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
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_recharge_order_status_create`(`status` ASC, `create_time` ASC, `out_trade_no` ASC) USING BTREE,
  INDEX `idx_recharge_order_user_status_create`(`user_id` ASC, `status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_recharge_order_create_id`(`create_time` ASC, `out_trade_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '充值订单表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 77 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统全局动态配置表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能分析文档表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for test
-- ----------------------------
DROP TABLE IF EXISTS `test`;
CREATE TABLE `test`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '测试记录编号',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NULL DEFAULT NULL COMMENT '用户名',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人编号',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人编号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10000003 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '测试数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for test_no_index
-- ----------------------------
DROP TABLE IF EXISTS `test_no_index`;
CREATE TABLE `test_no_index`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `create_user` bigint NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `update_user` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10000003 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for test_with_index
-- ----------------------------
DROP TABLE IF EXISTS `test_with_index`;
CREATE TABLE `test_with_index`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `create_user` bigint NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `update_user` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_update_user`(`update_user` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6322 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for u_account
-- ----------------------------
DROP TABLE IF EXISTS `u_account`;
CREATE TABLE `u_account`  (
  `user_id` int NOT NULL COMMENT '用户ID，关联用户表',
  `balance` decimal(18, 2) NOT NULL DEFAULT 0.00 COMMENT '当前余额',
  `freeze_amount` decimal(18, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额（处理中或提现中）',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户账户余额表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for u_account_log
-- ----------------------------
DROP TABLE IF EXISTS `u_account_log`;
CREATE TABLE `u_account_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '资金流水编号',
  `user_id` int NOT NULL COMMENT '所属用户ID',
  `type` tinyint NOT NULL COMMENT '变动类型：1-充值、2-发红包、3-抢红包、4-AI算力、5-人工调账、6-过期退回、7-转账',
  `amount` decimal(18, 2) NOT NULL COMMENT '变动金额（正数为进账，负数为出账）',
  `balance_after` decimal(18, 2) NOT NULL COMMENT '变动后的余额（用于后期对账）',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联的业务单号（转账单号或红包单号）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_account_log_user_type_time`(`user_id` ASC, `type` ASC, `create_time` ASC, `id` ASC) USING BTREE,
  INDEX `idx_account_log_type_time`(`type` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 113 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '资金变动流水表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for u_red_packet
-- ----------------------------
DROP TABLE IF EXISTS `u_red_packet`;
CREATE TABLE `u_red_packet`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '红包编号',
  `sender_id` bigint NOT NULL COMMENT '发红包的用户ID',
  `group_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联的群聊或房间ID',
  `total_amount` decimal(18, 2) NOT NULL COMMENT '红包总金额',
  `total_count` int NOT NULL COMMENT '红包总个数',
  `remaining_amount` decimal(18, 2) NOT NULL COMMENT '剩余金额',
  `remaining_count` int NOT NULL COMMENT '剩余个数',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '红包状态：0-进行中、1-已抢完、2-已退回（过期后）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `finish_time` datetime NULL DEFAULT NULL COMMENT '抢完的时间',
  `receiver_id` bigint NULL DEFAULT NULL COMMENT '指定接收人的 ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sender_id`(`sender_id` ASC) USING BTREE,
  INDEX `idx_room_id`(`group_id` ASC) USING BTREE,
  INDEX `idx_red_packet_status_create`(`status` ASC, `create_time` ASC, `id` ASC) USING BTREE,
  INDEX `idx_red_packet_create_id`(`create_time` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '红包主表' ROW_FORMAT = DYNAMIC;

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
  INDEX `idx_red_packet_id`(`red_packet_id` ASC) USING BTREE,
  INDEX `idx_red_packet_record_packet_time`(`red_packet_id` ASC, `create_time` ASC, `id` ASC) USING BTREE,
  INDEX `idx_red_packet_record_create_id`(`create_time` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2099819320975917059 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '红包领取记录明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for u_transfer
-- ----------------------------
DROP TABLE IF EXISTS `u_transfer`;
CREATE TABLE `u_transfer`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '转账记录编号',
  `transfer_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '转账业务单号',
  `sender_id` int NOT NULL COMMENT '转出用户编号',
  `receiver_id` int NOT NULL COMMENT '收款用户编号',
  `amount` decimal(18, 2) NOT NULL COMMENT '转账金额',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '转账状态（0 待接收、1 已接收、2 已退回）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '转账备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `accept_time` datetime NULL DEFAULT NULL COMMENT '接收时间',
  `return_time` datetime NULL DEFAULT NULL COMMENT '退回时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_transfer_no`(`transfer_no` ASC) USING BTREE,
  INDEX `idx_transfer_sender`(`sender_id` ASC) USING BTREE,
  INDEX `idx_transfer_receiver_status`(`receiver_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_transfer_expire`(`status` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户转账记录' ROW_FORMAT = DYNAMIC;

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
  UNIQUE INDEX `email`(`email` ASC) USING BTREE,
  INDEX `idx_user_status_id`(`status` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
