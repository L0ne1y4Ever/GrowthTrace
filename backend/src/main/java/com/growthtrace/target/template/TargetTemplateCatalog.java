package com.growthtrace.target.template;

import com.growthtrace.common.enums.RequirementType;
import com.growthtrace.common.enums.TargetType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 内置目标模板：V1 三类 target_type × 若干模板 × 若干预置 requirement。
 * Step 6 target 模块细化时会被 Controller 作为"模板选项"下发给前端。
 */
@Component
public class TargetTemplateCatalog {

    public record RequirementTemplate(String reqName, RequirementType reqType, String description) {
    }

    public record TargetTemplate(
            String templateKey,
            TargetType targetType,
            String title,
            String description,
            List<RequirementTemplate> defaultRequirements
    ) {
    }

    public List<TargetTemplate> all() {
        return List.of(
                new TargetTemplate(
                        "job_seeking_backend_java",
                        TargetType.JOB_SEEKING,
                        "求职：Java 后端开发",
                        "面向秋招的 Java 后端岗位准备",
                        List.of(
                                new RequirementTemplate("JVM 内存模型与调优基础", RequirementType.KNOWLEDGE, "能讲清 GC 流程与常见调优参数"),
                                new RequirementTemplate("Spring / Spring Boot", RequirementType.SKILL, "能独立开发一个完整后端服务"),
                                new RequirementTemplate("MySQL 索引与事务", RequirementType.KNOWLEDGE, "理解隔离级别与常见索引失效"),
                                new RequirementTemplate("一段可讲清的后端项目经历", RequirementType.EXPERIENCE, "含架构图与难点方案")
                        )
                ),
                new TargetTemplate(
                        "postgrad_cs_408",
                        TargetType.POSTGRAD,
                        "考研：计算机统考 408",
                        "数据结构 / 计算机组成原理 / 操作系统 / 计算机网络",
                        List.of(
                                new RequirementTemplate("数据结构核心题", RequirementType.KNOWLEDGE, "完成指定题集并达到正确率门槛"),
                                new RequirementTemplate("操作系统核心概念", RequirementType.KNOWLEDGE, "进程/线程、内存、IO"),
                                new RequirementTemplate("计算机网络模型", RequirementType.KNOWLEDGE, "TCP/IP 栈与典型题"),
                                new RequirementTemplate("真题两轮过", RequirementType.EXPERIENCE, "近 10 年真题完成 2 轮")
                        )
                ),
                new TargetTemplate(
                        "skill_ai_engineering",
                        TargetType.SKILL_GROWTH,
                        "技能成长：AI 应用工程化",
                        "围绕 LLM 应用做一次端到端的小项目",
                        List.of(
                                new RequirementTemplate("Prompt 工程基础", RequirementType.KNOWLEDGE, "结构化输出、few-shot、函数调用"),
                                new RequirementTemplate("一次可演示的 AI 小项目", RequirementType.EXPERIENCE, "包含前后端、AI 调用、失败降级"),
                                new RequirementTemplate("向量检索基础", RequirementType.KNOWLEDGE, "理解 embedding 与向量检索场景")
                        )
                )
        );
    }

    public Map<TargetType, List<TargetTemplate>> groupedByType() {
        return all().stream().collect(java.util.stream.Collectors.groupingBy(TargetTemplate::targetType));
    }
}
