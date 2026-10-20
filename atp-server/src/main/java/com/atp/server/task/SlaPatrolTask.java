package com.atp.server.task;

import com.atp.pojo.entity.ticket.SlaTimer;
import com.atp.server.service.SlaEscalationService;
import com.atp.server.service.SlaTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SLA 巡检。每 5 分钟一趟：先刷新状态，再把超期没升级的升掉。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SlaPatrolTask {

    private final SlaTimerService slaTimerService;
    private final SlaEscalationService slaEscalationService;

    /**
     * 巡检一趟：先刷新计时器状态，再把超期没升级的升掉
     */
    @Scheduled(cron = "${atp.sla.patrol-cron:0 */5 * * * *}")
    public void patrol() {
        try {
            slaTimerService.refreshStates();
        } catch (Exception e) {
            log.error("SLA 状态刷新失败", e);
        }

        for (SlaTimer timer : slaTimerService.breachedNotEscalated()) {
            try {
                slaEscalationService.escalate(timer);
            } catch (Exception e) {
                // 单条失败不能影响其他工单，记完日志接着跑下一条
                log.error("SLA 升级失败，timerId={}", timer.getId(), e);
            }
        }
    }
}
