package com.growthtrace.target.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.growthtrace.common.exception.BusinessException;
import com.growthtrace.common.result.ResultCode;
import com.growthtrace.profile.entity.GrowthProfile;
import com.growthtrace.profile.mapper.GrowthProfileMapper;
import com.growthtrace.profile.service.ProfileCompletenessCalculator;
import com.growthtrace.target.dto.CreateTargetRequest;
import com.growthtrace.target.dto.RequirementPayload;
import com.growthtrace.target.dto.UpdateRequirementStatusRequest;
import com.growthtrace.target.dto.UpdateTargetRequest;
import com.growthtrace.target.dto.UpdateTargetStatusRequest;
import com.growthtrace.target.entity.GrowthTarget;
import com.growthtrace.target.entity.TargetRequirement;
import com.growthtrace.target.mapper.GrowthTargetMapper;
import com.growthtrace.target.mapper.TargetRequirementMapper;
import com.growthtrace.target.service.TargetService;
import com.growthtrace.target.template.TargetTemplateCatalog;
import com.growthtrace.target.vo.RequirementView;
import com.growthtrace.target.vo.TargetDetailView;
import com.growthtrace.target.vo.TargetTemplateVO;
import com.growthtrace.target.vo.TargetView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TargetServiceImpl implements TargetService {

    private final GrowthTargetMapper targetMapper;
    private final TargetRequirementMapper requirementMapper;
    private final TargetTemplateCatalog templateCatalog;
    private final ProfileCompletenessCalculator completenessCalculator;
    private final GrowthProfileMapper profileMapper;

    // ---------------- templates ----------------

    @Override
    public List<TargetTemplateVO> listTemplates() {
        return templateCatalog.all().stream()
                .map(t -> TargetTemplateVO.builder()
                        .templateKey(t.templateKey())
                        .targetType(t.targetType().name())
                        .title(t.title())
                        .description(t.description())
                        .defaultRequirements(t.defaultRequirements().stream()
                                .map(r -> TargetTemplateVO.RequirementTemplateVO.builder()
                                        .reqName(r.reqName())
                                        .reqType(r.reqType().name())
                                        .description(r.description())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    // ---------------- targets ----------------

    @Override
    @Transactional
    public TargetDetailView create(Long userId, CreateTargetRequest payload) {
        GrowthTarget t = new GrowthTarget();
        t.setUserId(userId);
        t.setTargetType(payload.getTargetType());
        t.setTitle(payload.getTitle().trim());
        t.setDescription(payload.getDescription());
        t.setTemplateKey(StringUtils.hasText(payload.getTemplateKey()) ? payload.getTemplateKey() : null);
        t.setStatus("ACTIVE");
        t.setDeadline(payload.getDeadline());
        t.setIsPrimary(0);
        targetMapper.insert(t);

        if (payload.getRequirements() != null) {
            int order = 0;
            for (RequirementPayload rp : payload.getRequirements()) {
                if (rp == null || !StringUtils.hasText(rp.getReqName())) continue;
                TargetRequirement r = buildRequirement(t.getId(), rp, order);
                requirementMapper.insert(r);
                order += 10;
            }
        }

        if (Boolean.TRUE.equals(payload.getIsPrimary())) {
            doSetPrimary(userId, t.getId());
            t.setIsPrimary(1);
        }

        refreshCompletenessCache(userId);

        log.info("target created: userId={}, targetId={}, primary={}", userId, t.getId(), t.getIsPrimary());
        return assembleDetail(t);
    }

    @Override
    public List<TargetView> listTargets(Long userId, String statusFilter) {
        LambdaQueryWrapper<GrowthTarget> q = new LambdaQueryWrapper<GrowthTarget>()
                .eq(GrowthTarget::getUserId, userId)
                .orderByDesc(GrowthTarget::getIsPrimary)
                .orderByDesc(GrowthTarget::getUpdatedAt);
        if (StringUtils.hasText(statusFilter)) {
            q.eq(GrowthTarget::getStatus, statusFilter);
        }
        List<GrowthTarget> list = targetMapper.selectList(q);
        Map<Long, RequirementStats> stats = loadRequirementStats(list);
        return list.stream().map(t -> toView(t, stats.get(t.getId()))).toList();
    }

    @Override
    public TargetDetailView getDetail(Long userId, Long targetId) {
        GrowthTarget t = requireOwnedTarget(userId, targetId);
        return assembleDetail(t);
    }

    @Override
    @Transactional
    public TargetView update(Long userId, Long targetId, UpdateTargetRequest payload) {
        GrowthTarget t = requireOwnedTarget(userId, targetId);
        t.setTitle(payload.getTitle().trim());
        t.setDescription(payload.getDescription());
        t.setDeadline(payload.getDeadline());
        targetMapper.updateById(t);
        return toView(t, loadRequirementStats(List.of(t)).get(t.getId()));
    }

    @Override
    @Transactional
    public TargetView updateStatus(Long userId, Long targetId, UpdateTargetStatusRequest payload) {
        GrowthTarget t = requireOwnedTarget(userId, targetId);
        t.setStatus(payload.getStatus());
        if ("ACHIEVED".equals(payload.getStatus()) && t.getAchievedAt() == null) {
            t.setAchievedAt(LocalDateTime.now());
        }
        if (!"ACHIEVED".equals(payload.getStatus())) {
            t.setAchievedAt(null);
        }
        // 非 ACTIVE 的目标不应保持 primary
        if (!"ACTIVE".equals(payload.getStatus())) {
            t.setIsPrimary(0);
        }
        targetMapper.updateById(t);
        refreshCompletenessCache(userId);
        return toView(t, loadRequirementStats(List.of(t)).get(t.getId()));
    }

    @Override
    @Transactional
    public void remove(Long userId, Long targetId) {
        GrowthTarget t = requireOwnedTarget(userId, targetId);
        targetMapper.deleteById(t.getId());
        requirementMapper.delete(new LambdaQueryWrapper<TargetRequirement>()
                .eq(TargetRequirement::getTargetId, targetId));
        refreshCompletenessCache(userId);
    }

    @Override
    @Transactional
    public TargetView setPrimary(Long userId, Long targetId) {
        GrowthTarget t = requireOwnedTarget(userId, targetId);
        if (!"ACTIVE".equals(t.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "仅 ACTIVE 目标可设为主目标");
        }
        doSetPrimary(userId, targetId);
        t.setIsPrimary(1);
        return toView(t, loadRequirementStats(List.of(t)).get(t.getId()));
    }

    private void doSetPrimary(Long userId, Long targetId) {
        targetMapper.update(null, new LambdaUpdateWrapper<GrowthTarget>()
                .eq(GrowthTarget::getUserId, userId)
                .ne(GrowthTarget::getId, targetId)
                .set(GrowthTarget::getIsPrimary, 0));
        targetMapper.update(null, new LambdaUpdateWrapper<GrowthTarget>()
                .eq(GrowthTarget::getUserId, userId)
                .eq(GrowthTarget::getId, targetId)
                .set(GrowthTarget::getIsPrimary, 1));
    }

    // ---------------- requirements ----------------

    @Override
    @Transactional
    public RequirementView addRequirement(Long userId, Long targetId, RequirementPayload payload) {
        requireOwnedTarget(userId, targetId);
        int nextOrder = (int) (requirementMapper.selectCount(new LambdaQueryWrapper<TargetRequirement>()
                .eq(TargetRequirement::getTargetId, targetId)) * 10 + 10);
        TargetRequirement r = buildRequirement(targetId, payload, nextOrder);
        requirementMapper.insert(r);
        refreshCompletenessCache(userId);
        return toRequirementView(requirementMapper.selectById(r.getId()));
    }

    @Override
    @Transactional
    public RequirementView updateRequirement(Long userId, Long targetId, Long reqId, RequirementPayload payload) {
        requireOwnedTarget(userId, targetId);
        TargetRequirement r = requirementMapper.selectById(reqId);
        if (r == null || !targetId.equals(r.getTargetId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "要求不存在或不属于该目标");
        }
        r.setReqName(payload.getReqName().trim());
        r.setReqType(payload.getReqType());
        r.setDescription(payload.getDescription());
        if (payload.getSortOrder() != null) r.setSortOrder(payload.getSortOrder());
        r.setDueDate(payload.getDueDate());
        r.setLinkedSkillId(payload.getLinkedSkillId());
        r.setLinkedExperienceId(payload.getLinkedExperienceId());
        if (StringUtils.hasText(payload.getStatus())) {
            r.setStatus(payload.getStatus());
        }
        if (payload.getProgress() != null) {
            r.setProgress(payload.getProgress());
        }
        requirementMapper.updateById(r);
        return toRequirementView(requirementMapper.selectById(r.getId()));
    }

    @Override
    @Transactional
    public RequirementView updateRequirementStatus(Long userId, Long targetId, Long reqId, UpdateRequirementStatusRequest payload) {
        requireOwnedTarget(userId, targetId);
        TargetRequirement r = requirementMapper.selectById(reqId);
        if (r == null || !targetId.equals(r.getTargetId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "要求不存在或不属于该目标");
        }
        r.setStatus(payload.getStatus());
        int progress = payload.getProgress() != null ? payload.getProgress() : progressFromStatus(payload.getStatus(), r.getProgress());
        r.setProgress(progress);
        requirementMapper.updateById(r);
        return toRequirementView(requirementMapper.selectById(r.getId()));
    }

    @Override
    @Transactional
    public void removeRequirement(Long userId, Long targetId, Long reqId) {
        requireOwnedTarget(userId, targetId);
        TargetRequirement r = requirementMapper.selectById(reqId);
        if (r == null || !targetId.equals(r.getTargetId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "要求不存在或不属于该目标");
        }
        requirementMapper.deleteById(reqId);
        refreshCompletenessCache(userId);
    }

    // ---------------- helpers ----------------

    private record RequirementStats(int total, int met) {}

    private GrowthTarget requireOwnedTarget(Long userId, Long targetId) {
        GrowthTarget t = targetMapper.selectById(targetId);
        if (t == null || !userId.equals(t.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标不存在或不属于当前用户");
        }
        return t;
    }

    private TargetDetailView assembleDetail(GrowthTarget t) {
        List<TargetRequirement> reqs = requirementMapper.selectList(new LambdaQueryWrapper<TargetRequirement>()
                .eq(TargetRequirement::getTargetId, t.getId())
                .orderByAsc(TargetRequirement::getSortOrder)
                .orderByAsc(TargetRequirement::getId));
        RequirementStats stats = new RequirementStats(
                reqs.size(),
                (int) reqs.stream().filter(r -> "MET".equals(r.getStatus())).count()
        );
        return TargetDetailView.builder()
                .target(toView(t, stats))
                .requirements(reqs.stream().map(TargetServiceImpl::toRequirementView).toList())
                .build();
    }

    private Map<Long, RequirementStats> loadRequirementStats(List<GrowthTarget> targets) {
        Map<Long, RequirementStats> res = new HashMap<>();
        if (targets.isEmpty()) return res;
        List<Long> ids = targets.stream().map(GrowthTarget::getId).toList();
        List<TargetRequirement> all = requirementMapper.selectList(new LambdaQueryWrapper<TargetRequirement>()
                .in(TargetRequirement::getTargetId, ids));
        Map<Long, List<TargetRequirement>> grouped = new HashMap<>();
        for (TargetRequirement r : all) {
            grouped.computeIfAbsent(r.getTargetId(), k -> new ArrayList<>()).add(r);
        }
        for (Long id : ids) {
            List<TargetRequirement> group = grouped.getOrDefault(id, List.of());
            int met = (int) group.stream().filter(r -> "MET".equals(r.getStatus())).count();
            res.put(id, new RequirementStats(group.size(), met));
        }
        return res;
    }

    private TargetRequirement buildRequirement(Long targetId, RequirementPayload payload, int defaultOrder) {
        TargetRequirement r = new TargetRequirement();
        r.setTargetId(targetId);
        r.setReqName(payload.getReqName().trim());
        r.setReqType(payload.getReqType());
        r.setDescription(payload.getDescription());
        r.setStatus(StringUtils.hasText(payload.getStatus()) ? payload.getStatus() : "TODO");
        r.setSortOrder(payload.getSortOrder() != null ? payload.getSortOrder() : defaultOrder);
        r.setDueDate(payload.getDueDate());
        r.setLinkedSkillId(payload.getLinkedSkillId());
        r.setLinkedExperienceId(payload.getLinkedExperienceId());
        r.setProgress(payload.getProgress() != null
                ? payload.getProgress()
                : progressFromStatus(r.getStatus(), 0));
        return r;
    }

    private void refreshCompletenessCache(Long userId) {
        int c = completenessCalculator.calculate(userId);
        profileMapper.update(null, new LambdaUpdateWrapper<GrowthProfile>()
                .eq(GrowthProfile::getUserId, userId)
                .set(GrowthProfile::getCompleteness, c));
        log.debug("completeness recomputed on target change: userId={}, value={}", userId, c);
    }

    private static int progressFromStatus(String status, Integer current) {
        int cur = current == null ? 0 : current;
        return switch (status) {
            case "TODO" -> Math.min(cur, 10);
            case "IN_PROGRESS" -> Math.max(cur, 50);
            case "MET" -> 100;
            default -> cur;
        };
    }

    private static TargetView toView(GrowthTarget t, RequirementStats stats) {
        return TargetView.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .targetType(t.getTargetType())
                .title(t.getTitle())
                .description(t.getDescription())
                .templateKey(t.getTemplateKey())
                .status(t.getStatus())
                .deadline(t.getDeadline())
                .achievedAt(t.getAchievedAt())
                .isPrimary(t.getIsPrimary() != null && t.getIsPrimary() == 1)
                .requirementCount(stats == null ? 0 : stats.total())
                .requirementMetCount(stats == null ? 0 : stats.met())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private static RequirementView toRequirementView(TargetRequirement r) {
        return RequirementView.builder()
                .id(r.getId())
                .targetId(r.getTargetId())
                .reqName(r.getReqName())
                .reqType(r.getReqType())
                .description(r.getDescription())
                .status(r.getStatus())
                .sortOrder(r.getSortOrder())
                .dueDate(r.getDueDate())
                .linkedSkillId(r.getLinkedSkillId())
                .linkedExperienceId(r.getLinkedExperienceId())
                .progress(r.getProgress())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
