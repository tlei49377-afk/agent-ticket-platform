/* ==========================================================================
   项目二：智能工单 Agent 平台 —— 建表脚本
   库名：agent_ticket
   字符集：utf8mb4 / utf8mb4_general_ci
   引擎：InnoDB
   说明：所有表都带 create_time / update_time；业务表用 deleted 做逻辑删除
   ========================================================================== */

DROP DATABASE IF EXISTS agent_ticket;
CREATE DATABASE agent_ticket
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;
USE agent_ticket;

/* ==========================================================================
   一、平台域（先建，其他域要引用 user_id）
   ========================================================================== */

-- 用户
CREATE TABLE sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username      VARCHAR(64)  NOT NULL                COMMENT '登录名',
    password      VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt）',
    name          VARCHAR(64)  NOT NULL                COMMENT '姓名',
    phone         VARCHAR(20)      NULL                COMMENT '手机号',
    email         VARCHAR(128)     NULL                COMMENT '邮箱',
    avatar        VARCHAR(255)     NULL                COMMENT '头像 URL',
    status        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 启用 0 禁用',
    last_login_at DATETIME         NULL                COMMENT '最后登录时间',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删 1 已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT '系统用户';

-- 角色
CREATE TABLE sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    code        VARCHAR(64) NOT NULL                COMMENT '角色编码：requester/agent/supervisor/admin',
    name        VARCHAR(64) NOT NULL                COMMENT '角色名称',
    description VARCHAR(255)    NULL                COMMENT '描述',
    status      TINYINT     NOT NULL DEFAULT 1      COMMENT '状态：1 启用 0 禁用',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB COMMENT '角色';

-- 权限点
CREATE TABLE sys_permission (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    code        VARCHAR(64) NOT NULL                COMMENT '权限码，如 ticket:list',
    name        VARCHAR(64) NOT NULL                COMMENT '权限名称',
    module      VARCHAR(32) NOT NULL                COMMENT '所属模块',
    description VARCHAR(255)    NULL                COMMENT '描述',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_module (module)
) ENGINE = InnoDB COMMENT '权限点';

-- 用户-角色
CREATE TABLE sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role (role_id)
) ENGINE = InnoDB COMMENT '用户角色关联';

-- 角色-权限
CREATE TABLE sys_role_permission (
    id            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id       BIGINT NOT NULL COMMENT '角色 ID',
    permission_id BIGINT NOT NULL COMMENT '权限 ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, permission_id),
    KEY idx_permission (permission_id)
) ENGINE = InnoDB COMMENT '角色权限关联';

-- 审计日志（工具执行与关键操作都写这里）
CREATE TABLE sys_audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT           NULL COMMENT '操作人 ID',
    username    VARCHAR(64)      NULL COMMENT '操作人姓名（冗余，方便查）',
    action      VARCHAR(64)  NOT NULL COMMENT '动作：TOOL_EXEC/APPROVE/LOGIN/...',
    tool_code   VARCHAR(64)      NULL COMMENT '工具编码（工具类动作才有）',
    target_type VARCHAR(32)      NULL COMMENT '目标类型：TICKET/USER/...',
    target_id   BIGINT           NULL COMMENT '目标 ID',
    detail      JSON             NULL COMMENT '详情（参数、结果摘要）',
    success     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功',
    error_msg   VARCHAR(500)     NULL COMMENT '失败原因',
    ip          VARCHAR(64)      NULL COMMENT '来源 IP',
    trace_id    VARCHAR(64)      NULL COMMENT '链路 ID',
    cost_ms     INT              NULL COMMENT '耗时毫秒',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_time (user_id, create_time),
    KEY idx_tool (tool_code),
    KEY idx_trace (trace_id)
) ENGINE = InnoDB COMMENT '审计日志';

-- 每日指标预聚合（看板读它，不扫大表）
CREATE TABLE sys_metric_daily (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    stat_date    DATE        NOT NULL COMMENT '统计日期',
    dim_type     VARCHAR(16) NOT NULL COMMENT '维度类型：ALL/AGENT/MODEL/TOOL',
    dim_value    VARCHAR(64) NOT NULL DEFAULT '' COMMENT '维度值（如 agent 编码或模型名）',
    call_count   INT         NOT NULL DEFAULT 0 COMMENT '调用次数',
    token_in     BIGINT      NOT NULL DEFAULT 0 COMMENT '输入 token',
    token_out    BIGINT      NOT NULL DEFAULT 0 COMMENT '输出 token',
    cost         DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '成本（元）',
    error_count  INT         NOT NULL DEFAULT 0 COMMENT '错误次数',
    avg_latency  INT         NOT NULL DEFAULT 0 COMMENT '平均耗时毫秒',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_date_dim (stat_date, dim_type, dim_value)
) ENGINE = InnoDB COMMENT '每日指标预聚合';

/* ==========================================================================
   二、工单域
   ========================================================================== */

-- 工单分类（树形，用 ancestors 存路径，避免递归查库）
CREATE TABLE ticket_category (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT      NOT NULL DEFAULT 0      COMMENT '父 ID，0 为根',
    name        VARCHAR(64) NOT NULL                COMMENT '分类名称',
    ancestors   VARCHAR(255) NOT NULL DEFAULT ''    COMMENT '祖先路径，如 0,1,3',
    sort        INT         NOT NULL DEFAULT 0      COMMENT '排序',
    default_priority TINYINT NOT NULL DEFAULT 3     COMMENT '默认优先级：1 紧急 2 高 3 中 4 低',
    status      TINYINT     NOT NULL DEFAULT 1      COMMENT '状态：1 启用 0 禁用',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_parent (parent_id)
) ENGINE = InnoDB COMMENT '工单分类';

-- 工单主表
CREATE TABLE ticket (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_no         VARCHAR(32)  NOT NULL                COMMENT '工单号，如 TK202610010001',
    title             VARCHAR(200) NOT NULL                COMMENT '标题',
    content           TEXT             NULL                COMMENT '问题描述',
    channel           VARCHAR(16)  NOT NULL DEFAULT 'web'  COMMENT '来源渠道：web/mail/api/phone/ai',
    category_id       BIGINT           NULL                COMMENT '分类 ID',
    priority          TINYINT      NOT NULL DEFAULT 3      COMMENT '优先级：1 紧急 2 高 3 中 4 低',
    status            TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 待受理 2 处理中 3 已挂起 4 待用户确认 5 已解决 6 已关闭',
    requester_id      BIGINT       NOT NULL                COMMENT '提交人 ID',
    assignee_id       BIGINT           NULL                COMMENT '当前处理人 ID',
    sla_policy_id     BIGINT           NULL                COMMENT 'SLA 策略 ID',
    respond_deadline  DATETIME         NULL                COMMENT '响应截止时间',
    resolve_deadline  DATETIME         NULL                COMMENT '解决截止时间',
    first_respond_at  DATETIME         NULL                COMMENT '首次响应时间',
    resolved_at       DATETIME         NULL                COMMENT '解决时间',
    closed_at         DATETIME         NULL                COMMENT '关闭时间',
    reopen_count      INT          NOT NULL DEFAULT 0      COMMENT '重开次数',
    satisfaction      TINYINT          NULL                COMMENT '满意度 1~5',
    satisfaction_note VARCHAR(500)     NULL                COMMENT '满意度评语',
    ai_handled        TINYINT      NOT NULL DEFAULT 0      COMMENT '是否 AI 参与处理',
    ai_run_id         BIGINT           NULL                COMMENT 'AI 处理对应的 run',
    create_by         BIGINT           NULL                COMMENT '创建人',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_status_priority (status, priority),
    KEY idx_assignee (assignee_id, status),
    KEY idx_requester (requester_id),
    KEY idx_create_time (create_time),
    KEY idx_resolve_deadline (resolve_deadline)
) ENGINE = InnoDB COMMENT '工单';

-- 工单消息（公开回复 / 内部备注）
CREATE TABLE ticket_message (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id   BIGINT      NOT NULL                COMMENT '工单 ID',
    role        VARCHAR(16) NOT NULL                COMMENT '角色：requester/agent/ai/system',
    sender_id   BIGINT          NULL                COMMENT '发送人 ID（system/ai 可为空）',
    content     TEXT        NOT NULL                COMMENT '内容',
    visible     VARCHAR(16) NOT NULL DEFAULT 'public' COMMENT '可见性：public 用户可见 / internal 内部备注',
    attachments JSON            NULL                COMMENT '附件列表',
    ai_run_id   BIGINT          NULL                COMMENT 'AI 生成时对应的 run',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_ticket_time (ticket_id, create_time),
    KEY idx_visible (ticket_id, visible)
) ENGINE = InnoDB COMMENT '工单消息';

-- 工单附件
CREATE TABLE ticket_attachment (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id   BIGINT       NOT NULL                COMMENT '工单 ID',
    message_id  BIGINT           NULL                COMMENT '所属消息 ID',
    file_name   VARCHAR(255) NOT NULL                COMMENT '原始文件名',
    file_url    VARCHAR(500) NOT NULL                COMMENT '访问 URL',
    file_size   BIGINT       NOT NULL DEFAULT 0      COMMENT '大小（字节）',
    file_type   VARCHAR(64)      NULL                COMMENT 'MIME 类型',
    upload_by   BIGINT           NULL                COMMENT '上传人',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_ticket (ticket_id)
) ENGINE = InnoDB COMMENT '工单附件';

-- 标签
CREATE TABLE ticket_tag (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(32) NOT NULL                COMMENT '标签名',
    color       VARCHAR(16)     NULL                COMMENT '颜色',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB COMMENT '工单标签';

CREATE TABLE ticket_tag_rel (
    id        BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id BIGINT NOT NULL COMMENT '工单 ID',
    tag_id    BIGINT NOT NULL COMMENT '标签 ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_tag (ticket_id, tag_id)
) ENGINE = InnoDB COMMENT '工单标签关联';

-- 工单关系（父子/关联/重复）
CREATE TABLE ticket_relation (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id     BIGINT      NOT NULL COMMENT '主工单',
    related_id    BIGINT      NOT NULL COMMENT '关联工单',
    relation_type VARCHAR(16) NOT NULL COMMENT '关系：parent/child/duplicate/related',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_ticket (ticket_id)
) ENGINE = InnoDB COMMENT '工单关系';

-- 状态流转日志（审计友好）
CREATE TABLE ticket_status_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id   BIGINT       NOT NULL                COMMENT '工单 ID',
    from_status TINYINT          NULL                COMMENT '原状态（创建时为 NULL）',
    to_status   TINYINT      NOT NULL                COMMENT '新状态',
    operator_id BIGINT           NULL                COMMENT '操作人（系统操作为空）',
    reason      VARCHAR(500)     NULL                COMMENT '原因',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_ticket_time (ticket_id, create_time)
) ENGINE = InnoDB COMMENT '工单状态流转日志';
/* ==========================================================================
   三、SLA 域
   ========================================================================== */

-- SLA 策略
CREATE TABLE sla_policy (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(64) NOT NULL                COMMENT '策略名称',
    priority        TINYINT     NOT NULL                COMMENT '适用优先级：1 紧急 2 高 3 中 4 低',
    respond_minutes INT         NOT NULL                COMMENT '响应时限（分钟）',
    resolve_minutes INT         NOT NULL                COMMENT '解决时限（分钟）',
    warn_percent    TINYINT     NOT NULL DEFAULT 20     COMMENT '临期阈值：剩余时间低于该百分比视为临期',
    escalate_role   VARCHAR(64)     NULL                COMMENT '超期升级通知的角色编码',
    enabled         TINYINT     NOT NULL DEFAULT 1      COMMENT '是否启用',
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_priority (priority, enabled)
) ENGINE = InnoDB COMMENT 'SLA 策略';

-- SLA 计时器
CREATE TABLE sla_timer (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id   BIGINT      NOT NULL                COMMENT '工单 ID',
    timer_type  VARCHAR(16) NOT NULL                COMMENT '类型：respond 响应型 / resolve 解决型',
    deadline    DATETIME    NOT NULL                COMMENT '截止时间',
    state       VARCHAR(16) NOT NULL DEFAULT 'normal' COMMENT '状态：normal 正常 / warn 临期 / breached 已超期',
    breached_at DATETIME        NULL                COMMENT '实际超期时间',
    escalated   TINYINT     NOT NULL DEFAULT 0      COMMENT '是否已执行升级（幂等标记）',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_type (ticket_id, timer_type),
    KEY idx_state_deadline (state, deadline)
) ENGINE = InnoDB COMMENT 'SLA 计时器';

-- SLA 升级日志
CREATE TABLE sla_escalation_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id    BIGINT       NOT NULL                COMMENT '工单 ID',
    timer_id     BIGINT           NULL                COMMENT '计时器 ID',
    rule_name    VARCHAR(64)      NULL                COMMENT '触发规则名',
    action       VARCHAR(255) NOT NULL                COMMENT '升级动作描述',
    from_priority TINYINT         NULL                COMMENT '原优先级',
    to_priority  TINYINT          NULL                COMMENT '新优先级',
    notify_to    VARCHAR(255)     NULL                COMMENT '通知对象',
    success      TINYINT      NOT NULL DEFAULT 1      COMMENT '是否成功',
    error_msg    VARCHAR(500)     NULL                COMMENT '失败原因',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_ticket (ticket_id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB COMMENT 'SLA 升级日志';

/* ==========================================================================
   四、知识库域
   ========================================================================== */

-- 知识库文档
CREATE TABLE kb_document (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title        VARCHAR(200) NOT NULL                COMMENT '文档标题',
    doc_type     VARCHAR(16)  NOT NULL DEFAULT 'file' COMMENT '类型：file 文档 / faq 词条集',
    source_url   VARCHAR(500)     NULL                COMMENT '来源 URL',
    file_path    VARCHAR(500)     NULL                COMMENT '文件存储路径',
    status       VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT '状态：pending/processing/ready/failed',
    chunk_count  INT          NOT NULL DEFAULT 0      COMMENT '分块数量',
    version      INT          NOT NULL DEFAULT 1      COMMENT '版本号',
    error_msg    VARCHAR(500)     NULL                COMMENT '处理失败原因',
    uploaded_by  BIGINT           NULL                COMMENT '上传人',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_doc_type (doc_type)
) ENGINE = InnoDB COMMENT '知识库文档';

-- 知识库分块（向量元数据）
CREATE TABLE kb_chunk (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    document_id     BIGINT       NOT NULL                COMMENT '文档 ID',
    seq             INT          NOT NULL                COMMENT '块序号',
    content         TEXT         NOT NULL                COMMENT '块内容',
    token_count     INT          NOT NULL DEFAULT 0      COMMENT 'token 数',
    vector_id       VARCHAR(128)     NULL                COMMENT 'Redis 中的向量 key',
    embedding_model VARCHAR(64)      NULL                COMMENT '向量模型名',
    content_hash    VARCHAR(64)  NOT NULL                COMMENT '内容 SHA-256，用于去重跳过',
    page_no         INT              NULL                COMMENT '页码（PDF 用）',
    category        VARCHAR(64)      NULL                COMMENT '业务分类',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_document (document_id, seq),
    UNIQUE KEY uk_content_hash (content_hash)
) ENGINE = InnoDB COMMENT '知识库分块';

-- FAQ 词条
CREATE TABLE kb_faq (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    question           VARCHAR(500) NOT NULL                COMMENT '标准问题',
    similar_questions  JSON             NULL                COMMENT '相似问法数组',
    answer             TEXT         NOT NULL                COMMENT '标准答案',
    category           VARCHAR(64)      NULL                COMMENT '分类',
    vector_id          VARCHAR(128)     NULL                COMMENT 'Redis 向量 key',
    content_hash       VARCHAR(64)      NULL                COMMENT '内容哈希，去重用',
    hit_count          INT          NOT NULL DEFAULT 0      COMMENT '命中次数',
    status             TINYINT      NOT NULL DEFAULT 1      COMMENT '1 启用 0 停用',
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted            TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_category (category),
    KEY idx_hash (content_hash)
) ENGINE = InnoDB COMMENT 'FAQ 词条';

-- 检索日志（评测与调参数据源）
CREATE TABLE kb_retrieval_log (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    query_text   VARCHAR(500) NOT NULL               COMMENT '查询语句',
    run_id       BIGINT          NULL                COMMENT '所属 run',
    mode         VARCHAR(16) NOT NULL DEFAULT 'hybrid' COMMENT '检索模式：vector/keyword/hybrid',
    top_k        INT         NOT NULL DEFAULT 5      COMMENT '返回条数',
    result_ids   JSON            NULL                COMMENT '命中的 chunk/faq id 列表（按序）',
    result_scores JSON           NULL                COMMENT '对应分数列表',
    top1_score   DECIMAL(6,4)    NULL                COMMENT '最高分',
    cost_ms      INT             NULL                COMMENT '耗时',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_run (run_id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB COMMENT '知识库检索日志';

/* ==========================================================================
   五、Agent 域
   ========================================================================== */

-- Agent 定义
CREATE TABLE agent (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code          VARCHAR(64)  NOT NULL                COMMENT '编码，如 presale/aftersale',
    name          VARCHAR(64)  NOT NULL                COMMENT '名称',
    description   VARCHAR(500)     NULL                COMMENT '描述',
    system_prompt TEXT             NULL                COMMENT '系统提示词（为空则用 classpath 下的默认）',
    model_name    VARCHAR(64)  NOT NULL DEFAULT 'qwen-plus' COMMENT '模型名',
    temperature   DECIMAL(3,2) NOT NULL DEFAULT 0.30    COMMENT '温度',
    max_rounds    INT          NOT NULL DEFAULT 6      COMMENT '最大模型轮次',
    kb_scope      JSON             NULL                COMMENT '可用知识库分类范围',
    tool_scope    JSON             NULL                COMMENT '可用工具编码列表',
    enabled       TINYINT      NOT NULL DEFAULT 1      COMMENT '是否启用',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB COMMENT 'Agent 定义';

-- 会话
CREATE TABLE agent_session (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    agent_id       BIGINT       NOT NULL                COMMENT 'Agent ID',
    user_id        BIGINT       NOT NULL                COMMENT '用户 ID',
    title          VARCHAR(200)     NULL                COMMENT '会话标题（取首句）',
    memory_id      VARCHAR(128) NOT NULL                COMMENT '记忆 ID，格式 {userId}:{sessionId}',
    ticket_id      BIGINT           NULL                COMMENT '关联工单（如有）',
    last_active_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    status         TINYINT      NOT NULL DEFAULT 1      COMMENT '1 活跃 0 归档',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_memory_id (memory_id),
    KEY idx_user_active (user_id, last_active_at),
    KEY idx_agent (agent_id)
) ENGINE = InnoDB COMMENT 'Agent 会话';

-- 消息
CREATE TABLE agent_message (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id   BIGINT      NOT NULL                COMMENT '会话 ID',
    run_id       BIGINT          NULL                COMMENT '所属 run',
    role         VARCHAR(16) NOT NULL                COMMENT '角色：user/ai/tool/system',
    content      TEXT            NULL                COMMENT '内容',
    tool_calls   JSON            NULL                COMMENT '模型发起的工具调用',
    tool_call_id VARCHAR(64)     NULL                COMMENT '工具结果对应的调用 ID',
    tool_code    VARCHAR(64)     NULL                COMMENT '工具编码',
    token_in     INT         NOT NULL DEFAULT 0      COMMENT '输入 token',
    token_out    INT         NOT NULL DEFAULT 0      COMMENT '输出 token',
    cost         DECIMAL(12,6) NOT NULL DEFAULT 0    COMMENT '成本（元）',
    latency_ms   INT         NOT NULL DEFAULT 0      COMMENT '耗时毫秒',
    seq          INT         NOT NULL DEFAULT 0      COMMENT '会话内序号（保证顺序）',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_session_seq (session_id, seq),
    KEY idx_run (run_id)
) ENGINE = InnoDB COMMENT 'Agent 消息';

-- 工具元数据
CREATE TABLE agent_tool (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code                VARCHAR(64)  NOT NULL                COMMENT '工具编码',
    name                VARCHAR(64)  NOT NULL                COMMENT '中文名',
    description         VARCHAR(1000) NOT NULL               COMMENT '给模型看的描述',
    param_schema        JSON             NULL                COMMENT '参数 JSON Schema',
    read_only           TINYINT      NOT NULL DEFAULT 1      COMMENT '是否只读',
    idempotent          TINYINT      NOT NULL DEFAULT 1      COMMENT '是否幂等',
    timeout_ms          INT          NOT NULL DEFAULT 5000   COMMENT '超时毫秒',
    max_retry           INT          NOT NULL DEFAULT 0      COMMENT '最大重试次数',
    retry_backoff_ms    INT          NOT NULL DEFAULT 500    COMMENT '重试退避基数',
    required_permission VARCHAR(64)      NULL                COMMENT '所需权限码',
    need_approval       TINYINT      NOT NULL DEFAULT 0      COMMENT '是否需要人工审批',
    external_system     VARCHAR(64)      NULL                COMMENT '外部系统名（外部工具才有）',
    enabled             TINYINT      NOT NULL DEFAULT 1      COMMENT '是否启用',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_readonly (read_only, enabled)
) ENGINE = InnoDB COMMENT 'Agent 工具元数据';

-- 运行记录
CREATE TABLE agent_run (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    run_no         VARCHAR(40) NOT NULL                COMMENT 'run 编号（业务展示用）',
    session_id     BIGINT      NOT NULL                COMMENT '会话 ID',
    agent_id       BIGINT      NOT NULL                COMMENT 'Agent ID',
    user_id        BIGINT      NOT NULL                COMMENT '发起人',
    trace_id       VARCHAR(64)     NULL                COMMENT '链路 ID',
    input_text     TEXT            NULL                COMMENT '用户输入',
    output_text    TEXT            NULL                COMMENT '最终输出',
    status         VARCHAR(24) NOT NULL DEFAULT 'running' COMMENT 'running/waiting_approval/success/failed/cancelled',
    node_count     INT         NOT NULL DEFAULT 0      COMMENT '经过的节点数',
    tool_call_count INT        NOT NULL DEFAULT 0      COMMENT '工具调用次数',
    round_count    INT         NOT NULL DEFAULT 0      COMMENT '模型轮次',
    token_in       INT         NOT NULL DEFAULT 0      COMMENT '输入 token 合计',
    token_out      INT         NOT NULL DEFAULT 0      COMMENT '输出 token 合计',
    cost           DECIMAL(12,6) NOT NULL DEFAULT 0    COMMENT '成本合计',
    latency_ms     INT         NOT NULL DEFAULT 0      COMMENT '总耗时',
    error_msg      VARCHAR(1000)   NULL                COMMENT '错误信息',
    prompt_version VARCHAR(32)     NULL                COMMENT '提示词版本',
    git_commit     VARCHAR(64)     NULL                COMMENT '代码版本',
    started_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    ended_at       DATETIME        NULL                COMMENT '结束时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_no (run_no),
    KEY idx_session (session_id, started_at),
    KEY idx_status (status),
    KEY idx_trace (trace_id),
    KEY idx_started (started_at)
) ENGINE = InnoDB COMMENT 'Agent 运行记录';

-- 运行步骤（状态图轨迹）
CREATE TABLE agent_run_step (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    run_id      BIGINT      NOT NULL                COMMENT 'run ID',
    seq         INT         NOT NULL                COMMENT '步骤序号',
    node        VARCHAR(32) NOT NULL                COMMENT '节点名：intake/retrieve/plan/tool/approval/respond/escalate',
    input_snap  JSON            NULL                COMMENT '输入快照',
    output_snap JSON            NULL                COMMENT '输出快照',
    tool_code   VARCHAR(64)     NULL                COMMENT '本步调用的工具',
    status      VARCHAR(16) NOT NULL DEFAULT 'ok'   COMMENT 'ok/error/interrupted',
    error_msg   VARCHAR(500)    NULL                COMMENT '错误信息',
    cost_ms     INT         NOT NULL DEFAULT 0      COMMENT '耗时毫秒',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_seq (run_id, seq),
    KEY idx_node (node)
) ENGINE = InnoDB COMMENT 'Agent 运行步骤';

-- 审批
CREATE TABLE agent_approval (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    run_id       BIGINT       NOT NULL                COMMENT 'run ID',
    session_id   BIGINT       NOT NULL                COMMENT '会话 ID',
    step_seq     INT          NOT NULL                COMMENT '发起审批的步骤序号',
    tool_code    VARCHAR(64)  NOT NULL                COMMENT '待执行的工具',
    arguments    JSON             NULL                COMMENT '工具参数',
    reason       VARCHAR(500)     NULL                COMMENT '申请理由（模型给出）',
    status       VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/expired',
    operator_id  BIGINT           NULL                COMMENT '审批人',
    operated_at  DATETIME         NULL                COMMENT '审批时间',
    comment      VARCHAR(500)     NULL                COMMENT '审批意见',
    expire_at    DATETIME         NULL                COMMENT '超时时间',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_status (status, create_time),
    KEY idx_run (run_id)
) ENGINE = InnoDB COMMENT 'AI 写操作审批';

-- 用户反馈
CREATE TABLE agent_feedback (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    run_id      BIGINT      NOT NULL                COMMENT 'run ID',
    session_id  BIGINT      NOT NULL                COMMENT '会话 ID',
    user_id     BIGINT      NOT NULL                COMMENT '评价人',
    rating      VARCHAR(8)  NOT NULL                COMMENT '评价：up/down',
    reason      VARCHAR(64)     NULL                COMMENT '原因分类：不准确/不完整/太啰嗦/其他',
    comment     VARCHAR(500)    NULL                COMMENT '补充说明',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_run (run_id),
    KEY idx_rating (rating)
) ENGINE = InnoDB COMMENT 'AI 回答反馈';

/* ==========================================================================
   六、评测域
   ========================================================================== */

CREATE TABLE eval_dataset (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(100) NOT NULL                COMMENT '数据集名称',
    description VARCHAR(500)     NULL                COMMENT '描述',
    agent_id    BIGINT           NULL                COMMENT '目标 Agent',
    case_count  INT          NOT NULL DEFAULT 0      COMMENT '用例数',
    version     VARCHAR(32)  NOT NULL DEFAULT 'v1'   COMMENT '版本',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT '评测数据集';

CREATE TABLE eval_case (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    dataset_id        BIGINT       NOT NULL                COMMENT '数据集 ID',
    category          VARCHAR(32)  NOT NULL                COMMENT '类别：kb/query/write/escalate/edge',
    question          VARCHAR(1000) NOT NULL               COMMENT '测试问题',
    expected_tools    JSON             NULL                COMMENT '期望调用的工具编码数组',
    expected_keywords JSON             NULL                COMMENT '期望答案包含的关键词',
    expected_doc_ids  JSON             NULL                COMMENT '期望命中的文档 ID',
    expected_escalate TINYINT      NOT NULL DEFAULT 0      COMMENT '是否期望转人工',
    expected_approval TINYINT      NOT NULL DEFAULT 0      COMMENT '是否期望触发审批',
    weight            DECIMAL(4,2) NOT NULL DEFAULT 1.00   COMMENT '权重',
    remark            VARCHAR(500)     NULL                COMMENT '备注',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_dataset (dataset_id, category)
) ENGINE = InnoDB COMMENT '评测用例';

CREATE TABLE eval_run (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    dataset_id     BIGINT       NOT NULL                COMMENT '数据集 ID',
    agent_id       BIGINT       NOT NULL                COMMENT 'Agent ID',
    model_name     VARCHAR(64)      NULL                COMMENT '模型名',
    prompt_version VARCHAR(32)      NULL                COMMENT '提示词版本',
    git_commit     VARCHAR(64)      NULL                COMMENT '代码版本',
    status         VARCHAR(16)  NOT NULL DEFAULT 'running' COMMENT 'running/success/failed',
    total_cases    INT          NOT NULL DEFAULT 0      COMMENT '用例总数',
    passed_cases   INT          NOT NULL DEFAULT 0      COMMENT '通过数',
    tool_hit_rate     DECIMAL(6,4)  NULL COMMENT '工具命中率',
    keyword_hit_rate  DECIMAL(6,4)  NULL COMMENT '关键词命中率',
    doc_recall        DECIMAL(6,4)  NULL COMMENT '文档召回率',
    mrr               DECIMAL(6,4)  NULL COMMENT 'MRR',
    avg_faithfulness  DECIMAL(4,2)  NULL COMMENT '平均忠实度',
    avg_relevance     DECIMAL(4,2)  NULL COMMENT '平均相关性',
    avg_rounds        DECIMAL(4,2)  NULL COMMENT '平均轮次',
    avg_cost          DECIMAL(12,6) NULL COMMENT '平均成本',
    avg_latency_ms    INT           NULL COMMENT '平均耗时',
    total_token       BIGINT        NULL COMMENT '总 token',
    started_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    ended_at       DATETIME         NULL                COMMENT '结束时间',
    PRIMARY KEY (id),
    KEY idx_dataset (dataset_id, started_at)
) ENGINE = InnoDB COMMENT '评测批次';

CREATE TABLE eval_result (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    eval_run_id    BIGINT       NOT NULL                COMMENT '评测批次 ID',
    case_id        BIGINT       NOT NULL                COMMENT '用例 ID',
    actual_answer  TEXT             NULL                COMMENT '实际答案',
    actual_tools   JSON             NULL                COMMENT '实际调用的工具',
    actual_doc_ids JSON             NULL                COMMENT '实际命中的文档',
    escalate       TINYINT      NOT NULL DEFAULT 0      COMMENT '是否转人工',
    approval       TINYINT      NOT NULL DEFAULT 0      COMMENT '是否触发审批',
    tool_hit       TINYINT          NULL                COMMENT '工具是否命中',
    keyword_hit    TINYINT          NULL                COMMENT '关键词是否命中',
    doc_hit        TINYINT          NULL                COMMENT '文档是否命中',
    judge_score    DECIMAL(4,2)     NULL                COMMENT '裁判总分',
    judge_faithfulness DECIMAL(4,2) NULL                COMMENT '忠实度分',
    judge_relevance    DECIMAL(4,2) NULL                COMMENT '相关性分',
    judge_reason   VARCHAR(1000)    NULL                COMMENT '裁判理由',
    token_in       INT          NOT NULL DEFAULT 0      COMMENT '输入 token',
    token_out      INT          NOT NULL DEFAULT 0      COMMENT '输出 token',
    latency_ms     INT          NOT NULL DEFAULT 0      COMMENT '耗时',
    error_msg      VARCHAR(500)     NULL                COMMENT '错误',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_run (eval_run_id),
    KEY idx_case (case_id)
) ENGINE = InnoDB COMMENT '评测结果明细';

/* ==========================================================================
   结束
   ========================================================================== */
