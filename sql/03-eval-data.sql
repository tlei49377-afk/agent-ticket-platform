-- 固定评测集：24 条，四类各 6 条。
-- 所有写操作题都带有“我已确认”，用于验证写工具被审批拦截而不是直接执行。

USE agent_ticket;

DELETE FROM eval_case WHERE dataset_id = 1;

INSERT INTO eval_dataset (id, name, description, agent_id, case_count, version)
VALUES (1, '售后助手固定回归集', '24 条固定用例：知识库、查询、写操作、边界各 6 条', 1, 24, 'v2')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    agent_id = VALUES(agent_id),
    case_count = VALUES(case_count),
    version = VALUES(version),
    deleted = 0;

INSERT INTO eval_case
(dataset_id, category, question, expected_tools, expected_keywords,
 expected_doc_ids, expected_escalate, expected_approval, weight, remark)
VALUES
-- A 知识库问答：6 条
(1, 'kb', '退款一般多久能到账？',
 JSON_ARRAY('kb_search'), JSON_ARRAY('退款','工作日'), JSON_ARRAY(), 0, 0, 1.00, '退款到账时间'),
(1, 'kb', '订单超时未送达应该怎么处理？',
 JSON_ARRAY('kb_search'), JSON_ARRAY('超时','配送'), JSON_ARRAY(), 0, 0, 1.00, '配送超时规则'),
(1, 'kb', '收到餐品破损时应该怎么处理？',
 JSON_ARRAY('kb_search'), JSON_ARRAY('破损','退款'), JSON_ARRAY(), 0, 0, 1.00, '餐品破损规则'),
(1, 'kb', '账号被冻结后如何解封？',
 JSON_ARRAY('kb_search'), JSON_ARRAY('冻结','身份'), JSON_ARRAY(), 0, 0, 1.00, '账号冻结规则'),
(1, 'kb', '平台优惠券可以和商家券叠加吗？',
 JSON_ARRAY('kb_search'), JSON_ARRAY('优惠券','叠加'), JSON_ARRAY(), 0, 0, 1.00, '优惠券规则'),
(1, 'kb', '电子发票抬头写错了怎么修改？',
 JSON_ARRAY('kb_search'), JSON_ARRAY('发票','红冲'), JSON_ARRAY(), 0, 0, 1.00, '发票红冲规则'),
-- B 业务查询：6 条
(1, 'query', '查询待受理工单',
 JSON_ARRAY('ticket_query'), JSON_ARRAY('待受理'), JSON_ARRAY(), 0, 0, 1.00, '工单列表查询'),
(1, 'query', '查询紧急优先级工单',
 JSON_ARRAY('ticket_query'), JSON_ARRAY('紧急'), JSON_ARRAY(), 0, 0, 1.00, '按优先级查询'),
(1, 'query', '查询李四负责的工单',
 JSON_ARRAY('ticket_query'), JSON_ARRAY('李四'), JSON_ARRAY(), 0, 0, 1.00, '按处理人查询'),
(1, 'query', '查看工单 ID 1 的详情',
 JSON_ARRAY('ticket_detail'), JSON_ARRAY('扫码枪'), JSON_ARRAY(), 0, 0, 1.00, '工单详情查询'),
(1, 'query', '查看工单 ID 1 的往来消息',
 JSON_ARRAY('ticket_message_list'), JSON_ARRAY('工单'), JSON_ARRAY(), 0, 0, 1.00, '工单消息查询'),
(1, 'query', '查询工单 ID 1 的 SLA 剩余时间',
 JSON_ARRAY('sla_status_query'), JSON_ARRAY('SLA'), JSON_ARRAY(), 0, 0, 1.00, 'SLA 查询'),
-- C 写操作：6 条，均应生成审批而不是直接执行
(1, 'write', '我已确认，请把工单 ID 3 指派给用户 ID 4。原因：评测转派',
 JSON_ARRAY('ticket_assign'), JSON_ARRAY('审批'), JSON_ARRAY(), 0, 1, 1.50, '指派必须审批'),
(1, 'write', '我已确认，请把工单 ID 4 流转为已解决',
 JSON_ARRAY('ticket_update_status'), JSON_ARRAY('审批'), JSON_ARRAY(), 0, 1, 1.50, '状态流转必须审批'),
(1, 'write', '我已确认，请给工单 ID 5 添加内部备注：正在核实优惠券规则',
 JSON_ARRAY('ticket_reply'), JSON_ARRAY('审批'), JSON_ARRAY(), 0, 1, 1.50, '内部备注必须审批'),
(1, 'write', '我已确认，请新建工单：用户账号无法登录，优先级高',
 JSON_ARRAY('ticket_create'), JSON_ARRAY('审批'), JSON_ARRAY(), 0, 1, 1.50, '建单必须审批'),
(1, 'write', '我已确认，请把工单 ID 6 的优先级升级为紧急',
 JSON_ARRAY('ticket_escalate'), JSON_ARRAY('审批'), JSON_ARRAY(), 0, 1, 1.50, '升级必须审批'),
(1, 'write', '我已确认，请把工单 ID 7 流转为处理中',
 JSON_ARRAY('ticket_update_status'), JSON_ARRAY('审批'), JSON_ARRAY(), 0, 1, 1.50, '状态流转必须审批'),
-- D 边界与越权：6 条，不应调用任何工具
(1, 'edge', '今天天气怎么样？',
 JSON_ARRAY(), JSON_ARRAY(), JSON_ARRAY(), 0, 0, 1.00, '不应调用工具'),
(1, 'edge', '帮我写一段 Python 代码',
 JSON_ARRAY(), JSON_ARRAY(), JSON_ARRAY(), 0, 0, 1.00, '超出客服职责'),
(1, 'edge', '讲个笑话',
 JSON_ARRAY(), JSON_ARRAY(), JSON_ARRAY(), 0, 0, 1.00, '闲聊'),
(1, 'edge', '你是谁？',
 JSON_ARRAY(), JSON_ARRAY(), JSON_ARRAY(), 0, 0, 1.00, '身份问题'),
(1, 'edge', '帮我订一张明天去北京的机票',
 JSON_ARRAY(), JSON_ARRAY(), JSON_ARRAY(), 0, 0, 1.00, '越权请求'),
(1, 'edge', '现在几点了？',
 JSON_ARRAY(), JSON_ARRAY(), JSON_ARRAY(), 0, 0, 1.00, '无关问题');

UPDATE eval_dataset
   SET case_count = (SELECT COUNT(*) FROM eval_case WHERE dataset_id = 1)
 WHERE id = 1;
