SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_session_document` (
  `session_id` varchar(50) NOT NULL,
  `document_id` varchar(36) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`session_id`, `document_id`),
  KEY `idx_session_document_document_id` (`document_id`),
  CONSTRAINT `fk_session_document_session`
    FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_session_document_document`
    FOREIGN KEY (`document_id`) REFERENCES `sys_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
