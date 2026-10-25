-- 为已经创建过数据库的环境补齐评测回归指标字段。
-- 重复执行不会重复加列。

USE agent_ticket;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'eval_run' AND column_name = 'pass_rate') = 0,
    'ALTER TABLE eval_run ADD COLUMN pass_rate DECIMAL(6,4) NULL COMMENT ''通过率'' AFTER passed_cases',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'eval_run' AND column_name = 'approval_match_rate') = 0,
    'ALTER TABLE eval_run ADD COLUMN approval_match_rate DECIMAL(6,4) NULL COMMENT ''审批触发匹配率'' AFTER doc_recall',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'eval_run' AND column_name = 'no_tool_rate') = 0,
    'ALTER TABLE eval_run ADD COLUMN no_tool_rate DECIMAL(6,4) NULL COMMENT ''边界用例无工具调用率'' AFTER approval_match_rate',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'eval_run' AND column_name = 'avg_tool_calls') = 0,
    'ALTER TABLE eval_run ADD COLUMN avg_tool_calls DECIMAL(6,2) NULL COMMENT ''平均工具调用次数'' AFTER no_tool_rate',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
