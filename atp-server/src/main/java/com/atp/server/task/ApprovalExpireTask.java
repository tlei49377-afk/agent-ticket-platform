package com.atp.server.task;

import com.atp.agent.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 审批单超时巡检。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApprovalExpireTask {

    private final ApprovalService approvalService;

    /**
     * 定期把超过有效期的 pending 审批单标记为 expired。
     */
    @Scheduled(fixedDelayString = "${atp.approval.expire-scan-ms:300000}")
    public void expireOutdated() {
        int count = approvalService.expireOutdated();
        if (count > 0) {
            log.info("已过期审批单数量：{}", count);
        }
    }
}
