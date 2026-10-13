package com.atp.server.service;

import com.atp.pojo.entity.ticket.SlaPolicy;

import java.util.List;

/**
 * SLA 策略。
 */
public interface SlaPolicyService {

    /**
     * 全部策略
     * @return
     */
    List<SlaPolicy> list();

    /**
     * 按 ID 查策略
     * @param id
     * @return
     */
    SlaPolicy getById(Long id);

    /**
     * 按优先级取启用中的策略，建单时算截止时间用
     * @param priority
     * @return
     */
    SlaPolicy getEnabledByPriority(Integer priority);

    /**
     * 新增策略
     * @param policy
     * @return
     */
    Long save(SlaPolicy policy);

    /**
     * 修改策略
     * @param policy
     */
    void update(SlaPolicy policy);

    /**
     * 删除策略
     * @param id
     */
    void delete(Long id);
}
