USE agent_ticket;

/* ---------- 角色 ---------- */
INSERT INTO sys_role (id, code, name, description) VALUES
 (1, 'requester',  '提交人', '提交工单并跟踪进度'),
 (2, 'agent',      '客服',   '处理工单、回复用户'),
 (3, 'supervisor', '主管',   '指派、审批 AI 写操作、看统计'),
 (4, 'admin',      '管理员', '配置 Agent、知识库、用户与权限');

/* ---------- 权限点 ---------- */
INSERT INTO sys_permission (id, code, name, module) VALUES
 (1,  'ticket:list',    '查看工单',     'ticket'),
 (2,  'ticket:add',     '创建工单',     'ticket'),
 (3,  'ticket:update',  '修改工单状态', 'ticket'),
 (4,  'ticket:assign',  '指派工单',     'ticket'),
 (5,  'ticket:reply',   '回复工单',     'ticket'),
 (6,  'ticket:approve', '审批 AI 操作', 'ticket'),
 (7,  'user:list',      '查看用户',     'user'),
 (8,  'user:manage',    '管理用户角色', 'user'),
 (9,  'kb:read',        '查看知识库',   'kb'),
 (10, 'kb:manage',      '管理知识库',   'kb'),
 (11, 'report:view',    '查看统计报表', 'report'),
 (12, 'agent:manage',   '配置 Agent',   'agent'),
 (13, 'eval:manage',    '管理评测',     'eval');

/* ---------- 用户（密码统一 123456） ---------- */
INSERT INTO sys_user (id, username, password, name, phone, status) VALUES
 (1, 'admin',   '$2a$10$K7zU94BnLscEpmcd24Pmm.mCF9qEXhnsgqxu6IxNrovvV/1.KE1aO', '系统管理员', '13800000001', 1),
 (2, 'zhangsan','$2a$10$K7zU94BnLscEpmcd24Pmm.mCF9qEXhnsgqxu6IxNrovvV/1.KE1aO', '张三',       '13800000002', 1),
 (3, 'lisi',    '$2a$10$K7zU94BnLscEpmcd24Pmm.mCF9qEXhnsgqxu6IxNrovvV/1.KE1aO', '李四',       '13800000003', 1),
 (4, 'wangwu',  '$2a$10$K7zU94BnLscEpmcd24Pmm.mCF9qEXhnsgqxu6IxNrovvV/1.KE1aO', '王五',       '13800000004', 1),
 (5, 'zhaoliu', '$2a$10$K7zU94BnLscEpmcd24Pmm.mCF9qEXhnsgqxu6IxNrovvV/1.KE1aO', '赵六',       '13800000005', 1);

/* ---------- 用户角色 ---------- */
INSERT INTO sys_user_role (user_id, role_id) VALUES
 (1, 4),          -- admin    -> 管理员
 (2, 3),          -- zhangsan -> 主管
 (3, 2), (4, 2),  -- lisi/wangwu -> 客服
 (5, 1);          -- zhaoliu  -> 提交人

/* ---------- 角色权限 ---------- */
-- 管理员：全部 13 个
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission;
-- 主管：除 agent:manage / eval:manage / user:manage 外全部
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE code NOT IN ('agent:manage','eval:manage','user:manage');
-- 客服：工单相关 + 知识库读
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission
WHERE code IN ('ticket:list','ticket:add','ticket:update','ticket:assign','ticket:reply','kb:read','user:list');
-- 提交人：只能看和建
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE code IN ('ticket:list','ticket:add');

/* ---------- 工单分类（注意 ancestors 存路径） ---------- */
INSERT INTO ticket_category (id, parent_id, name, ancestors, sort, default_priority) VALUES
 (1, 0, '账号问题',   '0',    1, 3),
 (2, 0, '订单问题',   '0',    2, 2),
 (3, 0, '退换货',     '0',    3, 2),
 (4, 0, '产品咨询',   '0',    4, 4),
 (5, 0, '投诉建议',   '0',    5, 2),
 (6, 3, '退款申请',   '0,3',  1, 2),
 (7, 3, '换货申请',   '0,3',  2, 3);

/* ---------- SLA 策略 ---------- */
INSERT INTO sla_policy (id, name, priority, respond_minutes, resolve_minutes, warn_percent, escalate_role) VALUES
 (1, '紧急工单策略', 1, 30,  240,  20, 'supervisor'),
 (2, '高优工单策略', 2, 60,  480,  20, 'supervisor'),
 (3, '普通工单策略', 3, 120, 1440, 20, 'supervisor'),
 (4, '低优工单策略', 4, 240, 2880, 20, 'supervisor');

/* ---------- 工具元数据（15 个，与 0.3 的工具清单一一对应） ---------- */
-- 只读工具
INSERT INTO agent_tool
 (code, name, description, read_only, idempotent, timeout_ms, max_retry, required_permission, need_approval) VALUES
 ('ticket_query','查工单列表','按状态、优先级、处理人、关键词查询工单列表。当用户询问有哪些工单、某人的工单、待处理工单时调用。',1,1,5000,2,'ticket:list',0),
 ('ticket_detail','查工单详情','根据工单 ID 或工单号查询单个工单的完整信息。',1,1,5000,2,'ticket:list',0),
 ('ticket_message_list','查工单往来消息','查询某个工单下的所有往来消息，可控制是否包含内部备注。',1,1,5000,2,'ticket:list',0),
 ('user_query','查用户','按姓名或角色查询系统用户。',1,1,5000,2,'user:list',0),
 ('sla_status_query','查 SLA 状态','查询工单的 SLA 剩余时间与超期情况。',1,1,5000,2,'ticket:list',0),
 ('kb_search','检索知识库','在知识库中做混合检索，返回相关文档片段及来源。',1,1,8000,1,'kb:read',0),
 ('faq_search','查常见问题','在 FAQ 词条库中匹配标准问答。',1,1,3000,1,'kb:read',0),
 ('stats_query','查统计指标','查询工单统计指标，如工单量、超期率、平均处理时长。',1,1,8000,1,'report:view',0);

-- 写工具（全部需审批，重试 0 次）
INSERT INTO agent_tool
 (code, name, description, read_only, idempotent, timeout_ms, max_retry, required_permission, need_approval) VALUES
 ('ticket_create','创建工单','创建一个新工单。写操作，执行前必须先向用户复述并取得同意。',0,1,5000,0,'ticket:add',1),
 ('ticket_assign','指派或转派工单','把工单指派或转派给某个客服。写操作，需先确认。',0,1,5000,0,'ticket:assign',1),
 ('ticket_update_status','流转工单状态','把工单流转到目标状态，只能走白名单。写操作，需先确认。',0,1,5000,0,'ticket:update',1),
 ('ticket_reply','添加工单回复','在工单下添加公开回复或内部备注。写操作，需先确认。',0,1,5000,0,'ticket:reply',1),
 ('ticket_escalate','升级工单优先级','提高工单优先级。写操作，需先确认。',0,1,5000,0,'ticket:update',1);

-- 外部工具
INSERT INTO agent_tool
 (code, name, description, read_only, idempotent, timeout_ms, max_retry, required_permission, need_approval, external_system) VALUES
 ('external_dish_query','查外卖系统菜品','查询外部外卖系统的菜品信息。',1,1,3000,1,'ticket:list',0,'sky-take-out'),
 ('external_order_query','查外卖系统订单','查询外部外卖系统的订单信息。',1,1,3000,1,'ticket:list',0,'sky-take-out');

/* ---------- Agent 定义 ---------- */
INSERT INTO agent (id, code, name, description, model_name, temperature, max_rounds, tool_scope, enabled) VALUES
 (1, 'aftersale', '售后工单助手', '面向售后场景，可查工单、检索知识库、在审批后执行工单流转',
  'qwen-plus', 0.30, 6,
  JSON_ARRAY('ticket_query','ticket_detail','ticket_message_list','user_query','sla_status_query',
             'kb_search','faq_search','stats_query','ticket_create','ticket_assign',
             'ticket_update_status','ticket_reply','ticket_escalate'), 1),
 (2, 'presale',   '售前咨询助手', '面向售前咨询，只能检索知识库和查外部系统数据，无写权限',
  'qwen-plus', 0.50, 4,
  JSON_ARRAY('kb_search','faq_search','external_dish_query','external_order_query'), 1);

/* ---------- 评测数据集占位（用例在阶段 6 灌） ---------- */
INSERT INTO eval_dataset (id, name, description, agent_id, case_count, version) VALUES
 (1, '售后助手基线评测集', '100 条，五类各 20 条', 1, 100, 'v1');
