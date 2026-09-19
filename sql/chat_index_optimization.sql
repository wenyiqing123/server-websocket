/*
 * 云聊 chat 数据库索引优化脚本
 *
 * 适用版本：MySQL 8.0+
 * 执行方式：在目标数据库执行本文件，例如：
 *   mysql -uroot -p chat < sql/chat_index_optimization.sql
 *
 * 说明：
 * 1. 本脚本只新增二级索引，不删除表、数据或现有索引。
 * 2. 每个索引都按名称检查，重复执行会自动跳过。
 * 3. 索引依据后端 MyBatis/MyBatis-Plus 查询、当前 chat 库 SHOW INDEX
 *    和 EXPLAIN 结果整理；test/test_no_index/test_with_index 为基准测试表，
 *    不属于业务表，因此不在本脚本中增加索引。
 * 4. message、u_account_log、u_red_packet 等大表新增索引会消耗磁盘并在
 *    首次创建时锁定/重建索引，请在业务低峰期执行并提前确认磁盘空间。
 */

USE `chat`;

DROP PROCEDURE IF EXISTS `add_index_if_missing`;
DELIMITER $$

CREATE PROCEDURE `add_index_if_missing`(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
            '` ADD INDEX `', REPLACE(p_index_name, '`', '``'),
            '` ', p_index_definition
        );
        PREPARE add_index_statement FROM @ddl;
        EXECUTE add_index_statement;
        DEALLOCATE PREPARE add_index_statement;
    END IF;
END$$

DELIMITER ;

/* AI 智能体、会话和客服知识库 */
CALL `add_index_if_missing`('ai_agent', 'idx_ai_agent_deleted_updated',
    '(`deleted`, `updated_at`, `id`)');

CALL `add_index_if_missing`('ai_agent', 'idx_ai_agent_owner_deleted_updated',
    '(`owner_user_id`, `deleted`, `updated_at`, `id`)');

CALL `add_index_if_missing`('ai_chat_memory', 'idx_ai_chat_memory_conversation_time',
    '(`conversation_id`, `timestamp`, `id`)');

CALL `add_index_if_missing`('ai_chat_session', 'idx_ai_chat_session_agent_owner',
    '(`agent_id`, `owner_user_id`)');

CALL `add_index_if_missing`('ai_chat_session', 'idx_ai_chat_session_ticket_time',
    '(`ticket_status`, `last_message_at`, `created_at`, `id`)');

CALL `add_index_if_missing`('ai_knowledge_document', 'idx_ai_knowledge_document_base_deleted_id',
    '(`knowledge_base_id`, `deleted`, `id`)');

CALL `add_index_if_missing`('ai_knowledge_document', 'idx_ai_knowledge_document_base_hash_deleted',
    '(`knowledge_base_id`, `content_hash`, `deleted`)');

CALL `add_index_if_missing`('ai_service_knowledge_binding', 'idx_ai_service_binding_mounted_priority',
    '(`mounted`, `priority`, `knowledge_base_id`)');

CALL `add_index_if_missing`('ai_service_question', 'idx_ai_service_question_warmup',
    '(`source_type`, `enabled`, `review_status`, `use_count`, `updated_at`, `id`)');

CALL `add_index_if_missing`('ai_service_question_log', 'idx_ai_service_question_log_matched_id',
    '(`matched`, `id`)');

/* AI 旧版 /chat 页面 */
CALL `add_index_if_missing`('conversation', 'idx_conversation_user_update',
    '(`user_name`, `update_time`, `id`)');

CALL `add_index_if_missing`('conversation_message', 'idx_conversation_message_conversation_id',
    '(`conversation_id`, `id`)');

/* 好友、群组及成员关系 */
CALL `add_index_if_missing`('friend', 'idx_friend_receiver_status_time',
    '(`friend_name`, `status`, `create_time`, `id`)');

CALL `add_index_if_missing`('friend', 'idx_friend_owner_status_time',
    '(`user_name`, `status`, `create_time`, `friend_name`)');

CALL `add_index_if_missing`('group_member', 'idx_group_member_user_group',
    '(`user_name`, `group_id`)');

CALL `add_index_if_missing`('chat_group', 'idx_chat_group_owner_id',
    '(`owner_name`, `id`)');

CALL `add_index_if_missing`('chat_group', 'idx_chat_group_create_id',
    '(`create_time`, `id`)');

/* 登录审计 */
CALL `add_index_if_missing`('login_audit_log', 'idx_login_audit_occurred_id',
    '(`occurred_at`, `id`)');

CALL `add_index_if_missing`('login_audit_log', 'idx_login_audit_remote_time',
    '(`remote_login`, `occurred_at`, `id`)');

/* 即时消息：群聊按接收方、后台/统计按时间范围查询 */
CALL `add_index_if_missing`('message', 'idx_message_to_send_id',
    '(`to_name`, `send_at`, `id`)');

CALL `add_index_if_missing`('message', 'idx_message_send_id',
    '(`send_at`, `id`)');

/* 初始化脚本中的 message 表没有该索引，而现有 chat 库可能已经有同名旧版本。 */
CALL `add_index_if_missing`('message', 'idx_from_to_send',
    '(`from_name`, `to_name`, `send_at`)');

CALL `add_index_if_missing`('message_intention', 'idx_message_intention_create_id',
    '(`create_time`, `id`)');

CALL `add_index_if_missing`('message_intention', 'idx_message_intention_tag_prompt',
    '(`tag_name`, `original_prompt`)');

/* 充值订单、钱包流水 */
CALL `add_index_if_missing`('recharge_order', 'idx_recharge_order_status_create',
    '(`status`, `create_time`, `out_trade_no`)');

CALL `add_index_if_missing`('recharge_order', 'idx_recharge_order_user_status_create',
    '(`user_id`, `status`, `create_time`)');

CALL `add_index_if_missing`('recharge_order', 'idx_recharge_order_create_id',
    '(`create_time`, `out_trade_no`)');

CALL `add_index_if_missing`('u_account_log', 'idx_account_log_user_type_time',
    '(`user_id`, `type`, `create_time`, `id`)');

CALL `add_index_if_missing`('u_account_log', 'idx_account_log_type_time',
    '(`type`, `create_time`)');

/* 红包统计、到期检查和红包明细 */
CALL `add_index_if_missing`('u_red_packet', 'idx_red_packet_status_create',
    '(`status`, `create_time`, `id`)');

CALL `add_index_if_missing`('u_red_packet', 'idx_red_packet_create_id',
    '(`create_time`, `id`)');

CALL `add_index_if_missing`('u_red_packet_record', 'idx_red_packet_record_packet_time',
    '(`red_packet_id`, `create_time`, `id`)');

CALL `add_index_if_missing`('u_red_packet_record', 'idx_red_packet_record_create_id',
    '(`create_time`, `id`)');

/* 用户审批/状态筛选；user_name、email 已有唯一索引，不重复创建。 */
CALL `add_index_if_missing`('user', 'idx_user_status_id',
    '(`status`, `id`)');

DROP PROCEDURE IF EXISTS `add_index_if_missing`;

/*
 * 可选的执行后检查：
 *   SHOW INDEX FROM message;
 *   SHOW INDEX FROM ai_chat_memory;
 *   SHOW INDEX FROM u_account_log;
 *   SHOW INDEX FROM u_red_packet;
 *   ANALYZE TABLE message, ai_chat_memory, u_account_log, u_red_packet;
 *
 * 注意：当前部分统计 SQL 使用 DATE(create_time)、LEFT(id, 16) 或前置通配符
 * LIKE '%关键词%'，这些写法会限制索引使用率。索引创建后如仍看到全表扫描，
 * 应进一步将日期条件改为 [start, end) 范围，并将 LEFT(id,16) 改为前缀范围
 * 或增加专用的生成列；本脚本不改变业务 SQL，避免影响现有行为。
 */
