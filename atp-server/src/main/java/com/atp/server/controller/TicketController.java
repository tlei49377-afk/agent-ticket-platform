package com.atp.server.controller;

import com.atp.common.annotation.RequirePermission;
import com.atp.common.result.PageResult;
import com.atp.common.result.Result;
import com.atp.pojo.dto.ticket.TicketAssignDTO;
import com.atp.pojo.dto.ticket.TicketCreateDTO;
import com.atp.pojo.dto.ticket.TicketPriorityDTO;
import com.atp.pojo.dto.ticket.TicketReplyDTO;
import com.atp.pojo.dto.ticket.TicketTransitDTO;
import com.atp.pojo.query.TicketPageQueryDTO;
import com.atp.pojo.vo.ticket.TicketDetailVO;
import com.atp.pojo.vo.ticket.TicketStatusLogVO;
import com.atp.pojo.vo.ticket.TicketVO;
import com.atp.server.interceptor.JwtTokenInterceptor;
import com.atp.server.service.TicketMessageService;
import com.atp.server.service.TicketService;
import com.atp.server.service.TicketStatusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单对外接口。路径与前端约定的完全一致，改路径要同步改前端。
 */
@RestController
@RequestMapping("/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;
    @Autowired
    private TicketStatusService ticketStatusService;
    @Autowired
    private TicketMessageService ticketMessageService;
    @Autowired
    private HttpServletRequest request;

    /**
     * 工单列表：分页 + 条件筛选
     * @param query
     * @return
     */
    @GetMapping
    @RequirePermission("ticket:list")
    public Result<PageResult<TicketVO>> page(TicketPageQueryDTO query) {
        return Result.success(ticketService.pageQuery(query, callerId()));
    }

    /**
     * 工单详情：含可流转状态和往来消息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @RequirePermission("ticket:list")
    public Result<TicketDetailVO> detail(@PathVariable Long id) {
        return Result.success(ticketService.detail(id, callerId()));
    }

    /**
     * 状态变更历史
     * @param id
     * @return
     */
    @GetMapping("/{id}/history")
    @RequirePermission("ticket:list")
    public Result<List<TicketStatusLogVO>> history(@PathVariable Long id) {
        return Result.success(ticketService.history(id, callerId()));
    }

    /**
     * 建单，返回新工单 ID
     * @param dto
     * @return
     */
    @PostMapping
    @RequirePermission("ticket:add")
    public Result<Long> create(@Valid @RequestBody TicketCreateDTO dto) {
        return Result.success(ticketService.create(dto, callerId()));
    }

    /**
     * 状态流转。能不能过去由状态机说了算，非法流转直接报错
     * @param id
     * @param dto
     * @return
     */
    @PutMapping("/{id}/status")
    @RequirePermission("ticket:update")
    public Result<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody TicketTransitDTO dto) {
        ticketStatusService.transit(id, dto.getTargetStatus(), callerId(), dto.getReason());
        return Result.success();
    }

    /**
     * 指派 / 转派
     * @param id
     * @param dto
     * @return
     */
    @PutMapping("/{id}/assignee")
    @RequirePermission("ticket:assign")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody TicketAssignDTO dto) {
        ticketService.assign(id, dto.getAssigneeId(), dto.getReason(), callerId());
        return Result.success();
    }

    /**
     * 调整优先级
     * @param id
     * @param dto
     * @return
     */
    @PutMapping("/{id}/priority")
    @RequirePermission("ticket:update")
    public Result<Void> changePriority(@PathVariable Long id, @Valid @RequestBody TicketPriorityDTO dto) {
        ticketService.changePriority(id, dto.getTargetPriority(), dto.getReason(), callerId());
        return Result.success();
    }

    /**
     * 回复工单：公开回复或内部备注
     * @param id
     * @param dto
     * @return
     */
    @PostMapping("/{id}/replies")
    @RequirePermission("ticket:reply")
    public Result<Long> reply(@PathVariable Long id, @Valid @RequestBody TicketReplyDTO dto) {
        return Result.success(ticketMessageService.reply(id, dto, callerId()));
    }

    /**
     * 逻辑删除工单
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @RequirePermission("ticket:update")
    public Result<Void> delete(@PathVariable Long id) {
        ticketService.delete(id, callerId());
        return Result.success();
    }

    /** 当前登录人来自 JWT 拦截器写进 request 的属性，不用 ThreadLocal
     *
     * */
    private Long callerId() {
        return (Long) request.getAttribute(JwtTokenInterceptor.ATTR_CALLER_ID);
    }
}
