package com.atp.server.controller;

import com.atp.common.annotation.RequirePermission;
import com.atp.common.result.Result;
import com.atp.pojo.entity.ticket.SlaPolicy;
import com.atp.server.service.SlaPolicyService;
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
 * SLA 策略配置接口。
 */
@RestController
@RequestMapping("/sla/policies")
public class SlaPolicyController {

    @Autowired
    private SlaPolicyService slaPolicyService;

    /**
     * 策略列表
     * @return
     */
    @GetMapping
    @RequirePermission("ticket:list")
    public Result<List<SlaPolicy>> list() {
        return Result.success(slaPolicyService.list());
    }

    /**
     * 新增策略
     * @param policy
     * @return
     */
    @PostMapping
    @RequirePermission("ticket:update")
    public Result<Long> save(@RequestBody SlaPolicy policy) {
        return Result.success(slaPolicyService.save(policy));
    }

    /**
     * 修改策略
     * @param policy
     * @return
     */
    @PutMapping
    @RequirePermission("ticket:update")
    public Result<Void> update(@RequestBody SlaPolicy policy) {
        slaPolicyService.update(policy);
        return Result.success();
    }

    /**
     * 删除策略
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @RequirePermission("ticket:update")
    public Result<Void> delete(@PathVariable Long id) {
        slaPolicyService.delete(id);
        return Result.success();
    }
}
