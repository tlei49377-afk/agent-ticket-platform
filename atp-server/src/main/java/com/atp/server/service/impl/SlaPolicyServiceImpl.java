package com.atp.server.service.impl;

import com.atp.common.constant.MessageConstant;
import com.atp.common.exception.BaseException;
import com.atp.pojo.entity.ticket.SlaPolicy;
import com.atp.server.mapper.SlaPolicyMapper;
import com.atp.server.service.SlaPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SLA 策略实现。
 */
@Service
@RequiredArgsConstructor
public class SlaPolicyServiceImpl implements SlaPolicyService {

    private final SlaPolicyMapper slaPolicyMapper;

    /**
     * 全部策略
     * @return
     */
    @Override
    public List<SlaPolicy> list() {
        return slaPolicyMapper.list();
    }

    /**
     * 按 ID 查策略
     * @param id
     * @return
     */
    @Override
    public SlaPolicy getById(Long id) {
        return id == null ? null : slaPolicyMapper.getById(id);
    }

    /**
     * 按优先级取启用中的策略
     * @param priority
     * @return
     */
    @Override
    public SlaPolicy getEnabledByPriority(Integer priority) {
        return priority == null ? null : slaPolicyMapper.getEnabledByPriority(priority);
    }

    /**
     * 新增策略，三个时限不能为空
     * @param policy
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(SlaPolicy policy) {
        if (policy.getPriority() == null || policy.getRespondMinutes() == null || policy.getResolveMinutes() == null) {
            throw new BaseException("优先级、响应时限、解决时限都不能为空");
        }
        if (policy.getWarnPercent() == null) {
            policy.setWarnPercent(20);
        }
        slaPolicyMapper.insert(policy);
        return policy.getId();
    }

    /**
     * 修改策略，先确认存在
     * @param policy
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SlaPolicy policy) {
        if (policy.getId() == null || slaPolicyMapper.getById(policy.getId()) == null) {
            throw new BaseException(MessageConstant.SLA_POLICY_NOT_FOUND);
        }
        slaPolicyMapper.update(policy);
    }

    /**
     * 删除策略，先确认存在
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (slaPolicyMapper.getById(id) == null) {
            throw new BaseException(MessageConstant.SLA_POLICY_NOT_FOUND);
        }
        slaPolicyMapper.deleteById(id);
    }
}
