// 工单状态 / 优先级 / 审批状态等枚举与配色，页面里统一从这里取

export const TICKET_STATUS = {
  1: { label: '待受理',    color: '#d97706' },
  2: { label: '处理中',    color: '#0f766e' },
  3: { label: '已挂起',    color: '#78716c' },
  4: { label: '待用户确认', color: '#7c3aed' },
  5: { label: '已解决',    color: '#15803d' },
  6: { label: '已关闭',    color: '#52525b' }
}

export const TICKET_PRIORITY = {
  1: { label: '紧急', color: '#dc2626' },
  2: { label: '高',   color: '#ea580c' },
  3: { label: '中',   color: '#ca8a04' },
  4: { label: '低',   color: '#65a30d' }
}

export const APPROVAL_STATUS = {
  pending:          { label: '待审批', color: '#d97706' },
  approved:         { label: '已通过', color: '#15803d' },
  executing:        { label: '执行中', color: '#2563eb' },
  executed:         { label: '已执行', color: '#15803d' },
  execution_failed: { label: '执行失败', color: '#dc2626' },
  rejected:         { label: '已驳回', color: '#dc2626' },
  expired:          { label: '已超时', color: '#78716c' }
}

export const RUN_STATUS = {
  running:          { label: '进行中', color: '#d97706' },
  waiting_approval: { label: '待审批', color: '#d97706' },
  success:          { label: '成功', color: '#15803d' },
  failed:           { label: '失败', color: '#dc2626' },
  cancelled:        { label: '已取消', color: '#78716c' }
}

export const EVAL_STATUS = {
  running: { label: '运行中', color: '#d97706' },
  success: { label: '已完成', color: '#15803d' },
  failed:  { label: '失败',   color: '#dc2626' }
}

// 后端返回的是数字或英文码，用这几个函数统一转成中文
export const statusOf   = (v) => TICKET_STATUS[v]   || { label: String(v ?? '-'), color: '#78716c' }
export const priorityOf = (v) => TICKET_PRIORITY[v] || { label: String(v ?? '-'), color: '#78716c' }
export const approvalOf = (v) => APPROVAL_STATUS[v] || { label: String(v ?? '-'), color: '#78716c' }
export const runOf      = (v) => RUN_STATUS[v]      || { label: String(v ?? '-'), color: '#78716c' }
export const evalOf     = (v) => EVAL_STATUS[v]     || { label: String(v ?? '-'), color: '#78716c' }

// 状态下拉框用（工单列表的筛选条件）
export const statusOptions   = Object.entries(TICKET_STATUS).map(([v, o]) => ({ value: Number(v), label: o.label }))
export const priorityOptions = Object.entries(TICKET_PRIORITY).map(([v, o]) => ({ value: Number(v), label: o.label }))

