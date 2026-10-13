package com.atp.server.statemachine;

import com.atp.common.constant.MessageConstant;
import com.atp.common.constant.StatusConstant;
import com.atp.common.exception.BaseException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态机。用白名单表控制流转，不在表里的一律拒绝。
 */
public final class TicketStateMachine {

    /**
     * 允许的流转：key 是当前状态，value 是可达状态集合。
     * 这张表与 docs/design/03-工单状态机.md 必须逐条一致。
     */
    private static final Map<Integer, Set<Integer>> TRANSITIONS;

    static {
        Map<Integer, Set<Integer>> m = new LinkedHashMap<>();
        m.put(StatusConstant.TICKET_PENDING,
                Set.of(StatusConstant.TICKET_PROCESSING, StatusConstant.TICKET_CLOSED));
        m.put(StatusConstant.TICKET_PROCESSING,
                Set.of(StatusConstant.TICKET_CONFIRMING, StatusConstant.TICKET_SUSPENDED, StatusConstant.TICKET_CLOSED));
        m.put(StatusConstant.TICKET_SUSPENDED,
                Set.of(StatusConstant.TICKET_PROCESSING));
        m.put(StatusConstant.TICKET_CONFIRMING,
                Set.of(StatusConstant.TICKET_RESOLVED, StatusConstant.TICKET_PROCESSING));
        m.put(StatusConstant.TICKET_RESOLVED,
                Set.of(StatusConstant.TICKET_CLOSED, StatusConstant.TICKET_PROCESSING));
        m.put(StatusConstant.TICKET_CLOSED,
                Set.of(StatusConstant.TICKET_PROCESSING));
        TRANSITIONS = Collections.unmodifiableMap(m);
    }

    private TicketStateMachine() {
    }

    /** 当前状态能去哪几个状态。前端拿它渲染流转按钮
     * @param from 当前状态
     * @return
     */
    public static Set<Integer> allowedNext(Integer from) {
        if (from == null) {
            return Set.of();
        }
        return TRANSITIONS.getOrDefault(from, Set.of());
    }

    /**
     * 这条流转允不允许
     * @param from 当前状态
     * @param to 下一状态
     * @return
     */
    public static boolean canTransfer(Integer from, Integer to) {
        // 先判 null 再 contains：Set.of(...) 对 null 会抛 NPE，不是返回 false
        return to != null && allowedNext(from).contains(to);
    }

    /** 非法流转直接抛业务异常，带上「从哪到哪」，排查时一眼能看懂
     * @param from 当前状态
     * @param to 下一状态
     */
    public static void check(Integer from, Integer to) {
        if (from == null || to == null) {
            throw new BaseException(MessageConstant.TICKET_STATUS_MISSING);
        }
        if (from.equals(to)) {
            throw new BaseException(MessageConstant.TICKET_STATUS_SAME);
        }
        if (!canTransfer(from, to)) {
            throw new BaseException(MessageConstant.TICKET_STATUS_ILLEGAL + "："
                    + StatusConstant.ticketStatusName(from) + " → " + StatusConstant.ticketStatusName(to));
        }
    }

    /** 已解决/已关闭又回到处理中，算一次重开
     * @param from 当前状态
     * @param to 下一状态
     * @return
     */
    public static boolean isReopen(Integer from, Integer to) {
        return from != null && to != null
                && from >= StatusConstant.TICKET_RESOLVED
                && to == StatusConstant.TICKET_PROCESSING;
    }

    /** 已关闭的单子不能回复，要沟通得先重开
     * @param status 状态
     * @return */
    public static boolean isFinal(Integer status) {
        return status != null && status == StatusConstant.TICKET_CLOSED;
    }
}
