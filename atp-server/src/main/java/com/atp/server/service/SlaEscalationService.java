package com.atp.server.service;

import com.atp.pojo.entity.ticket.SlaTimer;

/**
 * SLA 超期升级。
 */
public interface SlaEscalationService {

    /**
     * 升级动作必须幂等：重复调用只会生效一次
     * @param timer
     */
    void escalate(SlaTimer timer);
}
