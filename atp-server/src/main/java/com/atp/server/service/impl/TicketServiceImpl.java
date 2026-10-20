package com.atp.server.service.impl;

import com.atp.common.constant.MessageConstant;
import com.atp.common.constant.StatusConstant;
import com.atp.common.exception.BaseException;
import com.atp.common.exception.PermissionDeniedException;
import com.atp.common.result.PageResult;
import com.atp.pojo.dto.ticket.TicketCreateDTO;
import com.atp.pojo.entity.sys.SysUser;
import com.atp.pojo.entity.ticket.SlaPolicy;
import com.atp.pojo.entity.ticket.Ticket;
import com.atp.pojo.entity.ticket.TicketCategory;
import com.atp.pojo.entity.ticket.TicketStatusLog;
import com.atp.pojo.query.TicketPageQueryDTO;
import com.atp.pojo.vo.ticket.TicketDetailVO;
import com.atp.pojo.vo.ticket.TicketStatusLogVO;
import com.atp.pojo.vo.ticket.TicketVO;
import com.atp.server.mapper.SysUserMapper;
import com.atp.server.mapper.TicketMapper;
import com.atp.server.mapper.TicketStatusLogMapper;
import com.atp.server.service.SlaPolicyService;
import com.atp.server.service.SlaTimerService;
import com.atp.server.service.TicketCategoryService;
import com.atp.server.service.TicketMessageService;
import com.atp.server.service.TicketService;
import com.atp.server.service.UserAccessService;
import com.atp.server.statemachine.TicketStateMachine;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 工单读写实现。分页、数据隔离、建单时的 SLA 计算都在这里。
 */
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TicketMapper ticketMapper;
    private final TicketStatusLogMapper statusLogMapper;
    private final TicketCategoryService categoryService;
    private final TicketMessageService messageService;
    private final SlaPolicyService slaPolicyService;
    private final SlaTimerService slaTimerService;
    private final SysUserMapper sysUserMapper;
    private final UserAccessService userAccessService;

    /**
     * 分页查询。普通提交人只会查到自己的单子
     * @param query
     * @param currentUserId
     * @return
     */
    @Override
    public PageResult<TicketVO> pageQuery(TicketPageQueryDTO query, Long currentUserId) {
        // 数据隔离：不是内部人员，只能在「自己的工单」范围内查。
        // 前端就算传了别人的 requesterId 也没用，这里直接覆盖掉。
        Long scoped = userAccessService.scopeRequesterId(currentUserId);
        if (scoped != null) {
            query.setRequesterId(scoped);
        }

        PageHelper.startPage(query.getPage(), query.getPageSize());
        List<TicketVO> rows = ticketMapper.pageQuery(query);
        PageInfo<TicketVO> pageInfo = new PageInfo<>(rows);
        rows.forEach(this::fillLabels);
        return new PageResult<>(pageInfo.getTotal(), rows);
    }

    /**
     * 详情：校验可见性，再补状态中文名、可流转状态和消息
     * @param ticketId
     * @param currentUserId
     * @return
     */
    @Override
    public TicketDetailVO detail(Long ticketId, Long currentUserId) {
        requireVisible(ticketId, currentUserId);
        TicketDetailVO vo = ticketMapper.getVoById(ticketId);
        if (vo == null) {
            throw new BaseException(MessageConstant.TICKET_NOT_FOUND);
        }
        fillLabels(vo);
        // 下一跳由状态机给，前端只负责渲染按钮，不自己判断能去哪
        vo.setAllowedNextStatus(new ArrayList<>(TicketStateMachine.allowedNext(vo.getStatus())));
        vo.setMessages(messageService.list(ticketId, currentUserId));
        return vo;
    }

    /**
     * 状态变更历史
     * @param ticketId
     * @param currentUserId
     * @return
     */
    @Override
    public List<TicketStatusLogVO> history(Long ticketId, Long currentUserId) {
        requireVisible(ticketId, currentUserId);
        return statusLogMapper.listByTicket(ticketId);
    }

    /**
     * 建单：生成工单号、算 SLA 截止时间、开两个计时器、记一条初始流转日志
     * @param dto
     * @param currentUserId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TicketCreateDTO dto, Long currentUserId) {
        Ticket ticket = new Ticket();
        ticket.setTicketNo(nextTicketNo());
        ticket.setTitle(dto.getTitle());
        ticket.setContent(dto.getContent());
        ticket.setChannel(dto.getChannel() == null ? "web" : dto.getChannel());
        ticket.setStatus(StatusConstant.TICKET_PENDING);
        ticket.setRequesterId(currentUserId);
        ticket.setCreateBy(currentUserId);

        // 优先级：入参没传就用分类的默认值，分类也没配就按「中」
        Integer priority = dto.getPriority();
        if (dto.getCategoryId() != null) {
            TicketCategory category = categoryService.getById(dto.getCategoryId());
            if (category == null) {
                throw new BaseException(MessageConstant.TICKET_CATEGORY_NOT_FOUND);
            }
            ticket.setCategoryId(category.getId());
            if (priority == null) {
                priority = category.getDefaultPriority();
            }
        }
        if (priority == null) {
            priority = StatusConstant.PRIORITY_NORMAL;
        }
        if (priority < StatusConstant.PRIORITY_URGENT || priority > StatusConstant.PRIORITY_LOW) {
            throw new BaseException(MessageConstant.PRIORITY_ILLEGAL);
        }
        ticket.setPriority(priority);

        // SLA 从建单这一刻开始算，不是从分派那一刻
        SlaPolicy policy = slaPolicyService.getEnabledByPriority(priority);
        if (policy == null) {
            throw new BaseException(MessageConstant.SLA_POLICY_NOT_FOUND);
        }
        ticket.setSlaPolicyId(policy.getId());
        LocalDateTime now = LocalDateTime.now();
        ticket.setRespondDeadline(now.plusMinutes(policy.getRespondMinutes()));
        ticket.setResolveDeadline(now.plusMinutes(policy.getResolveMinutes()));

        // 下面三步必须同事务：工单、两条计时器、一条初始流转日志
        ticketMapper.insert(ticket);
        slaTimerService.createTimers(ticket, policy);
        writeLog(ticket.getId(), null, ticket.getStatus(), currentUserId, "创建工单");
        return ticket.getId();
    }

    /**
     * 指派 / 转派，记一条留痕
     * @param ticketId
     * @param assigneeId
     * @param reason
     * @param operatorId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long ticketId, Long assigneeId, String reason, Long operatorId) {
        Ticket ticket = requireTicket(ticketId);
        String assigneeName = nameOf(assigneeId);
        if (assigneeName == null) {
            throw new BaseException(MessageConstant.USER_NOT_FOUND);
        }
        ticketMapper.updateAssignee(ticketId, assigneeId);
        // 指派不改状态，但「谁把单给了谁」也要留痕，所以 from 和 to 写成同一个状态
        writeLog(ticketId, ticket.getStatus(), ticket.getStatus(), operatorId,
                "指派给 " + assigneeName + (reason == null || reason.isEmpty() ? "" : "：" + reason));
    }

    /**
     * 调整优先级，记一条留痕
     * @param ticketId
     * @param targetPriority
     * @param reason
     * @param operatorId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePriority(Long ticketId, Integer targetPriority, String reason, Long operatorId) {
        Ticket ticket = requireTicket(ticketId);
        if (targetPriority == null
                || targetPriority < StatusConstant.PRIORITY_URGENT
                || targetPriority > StatusConstant.PRIORITY_LOW) {
            throw new BaseException(MessageConstant.PRIORITY_ILLEGAL);
        }
        if (targetPriority.equals(ticket.getPriority())) {
            throw new BaseException(MessageConstant.PRIORITY_UNCHANGED);
        }
        ticketMapper.updatePriority(ticketId, targetPriority);
        writeLog(ticketId, ticket.getStatus(), ticket.getStatus(), operatorId,
                "优先级 " + StatusConstant.priorityName(ticket.getPriority())
                        + " 调整为 " + StatusConstant.priorityName(targetPriority)
                        + (reason == null || reason.isEmpty() ? "" : "：" + reason));
    }

    /**
     * 逻辑删除
     * @param ticketId
     * @param operatorId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long ticketId, Long operatorId) {
        requireVisible(ticketId, operatorId);
        ticketMapper.deleteById(ticketId);
    }

    /** 详情、历史、删除共用的越权校验
     * @param ticketId
     * @param currentUserId
     * @return
     * */
    private Ticket requireVisible(Long ticketId, Long currentUserId) {
        Ticket ticket = requireTicket(ticketId);
        if (!userAccessService.isInternalStaff(currentUserId)
                && !currentUserId.equals(ticket.getRequesterId())) {
            throw new PermissionDeniedException(MessageConstant.TICKET_NOT_OWNED);
        }
        return ticket;
    }

    /**
     * 取工单，取不到就抛业务异常
     * @param ticketId
     * @return
     */
    private Ticket requireTicket(Long ticketId) {
        Ticket ticket = ticketId == null ? null : ticketMapper.getById(ticketId);
        if (ticket == null) {
            throw new BaseException(MessageConstant.TICKET_NOT_FOUND);
        }
        return ticket;
    }

    /**
     * 工单号：TK + 当天日期 + 4 位当日序号。
     * 并发下靠 uk_ticket_no 唯一键兜底，真正的分布式序号等阶段 3 有了 Redis 再换 INCR。
     */
    private String nextTicketNo() {
        String prefix = "TK" + LocalDate.now().format(DAY);
        int todayCount = ticketMapper.countByNoPrefix(prefix);
        return prefix + String.format("%04d", todayCount + 1);
    }

    /** 状态码翻成中文，并算好 SLA 剩余分钟数
     * */
    private void fillLabels(TicketVO vo) {
        vo.setStatusLabel(StatusConstant.ticketStatusName(vo.getStatus()));
        vo.setPriorityLabel(StatusConstant.priorityName(vo.getPriority()));
        // 已经解决/关闭的单子不再显示倒计时，前端会把它当成已经不用管
        if (vo.getResolveDeadline() != null
                && vo.getStatus() != null
                && vo.getStatus() < StatusConstant.TICKET_RESOLVED) {
            vo.setSlaRemainMinutes(Duration.between(LocalDateTime.now(), vo.getResolveDeadline()).toMinutes());
        }
    }

    /**
     * 按用户 ID 取姓名，查不到返回 null
     * @param userId
     * @return
     */
    private String nameOf(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserMapper.getById(userId);
        return user == null ? null : user.getName();
    }

    /**
     * 写一条状态流转日志
     * @param ticketId
     * @param from
     * @param to
     * @param operatorId
     * @param reason
     */
    private void writeLog(Long ticketId, Integer from, Integer to, Long operatorId, String reason) {
        TicketStatusLog logRow = new TicketStatusLog();
        logRow.setTicketId(ticketId);
        logRow.setFromStatus(from);
        logRow.setToStatus(to);
        logRow.setOperatorId(operatorId);
        logRow.setReason(reason);
        statusLogMapper.insert(logRow);
    }
}
