package com.growthtrace.dashboard.service;

import com.growthtrace.dashboard.vo.HeatmapPoint;

import java.util.List;

/**
 * 活动热力图聚合：journal.created_at + task.check_in_dates + snapshot.snapshot_time 综合活动强度。
 * 每日一个 score；score = 该日出现的三类事件次数之和。
 */
public interface HeatmapService {

    /** 返回最近 windowDays 天（含今天）每天一个点（score 可能为 0）。windowDays 被限制在 [7, 365]。 */
    List<HeatmapPoint> compute(Long userId, int windowDays);
}
