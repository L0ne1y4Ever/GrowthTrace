package com.growthtrace.dashboard.service;

import com.growthtrace.dashboard.vo.DashboardOverviewView;
import com.growthtrace.dashboard.vo.GrowthCurvePoint;
import com.growthtrace.dashboard.vo.HeatmapPoint;

import java.util.List;

/**
 * Dashboard 聚合服务：组合 profile、target、task、journal、diagnosis、snapshot 的轻量视图。
 * 不新增业务状态，仅做聚合读取。
 */
public interface DashboardService {

    /** 聚合 7 大区块的总览视图；用户数据不完整时字段可能为 null/空。 */
    DashboardOverviewView overview(Long userId);

    /** 热力图（委托 HeatmapService）。 */
    List<HeatmapPoint> heatmap(Long userId, int windowDays);

    /** 成长曲线：按时间升序的 snapshot 关键指标。 */
    List<GrowthCurvePoint> growthCurve(Long userId, int limit);
}
