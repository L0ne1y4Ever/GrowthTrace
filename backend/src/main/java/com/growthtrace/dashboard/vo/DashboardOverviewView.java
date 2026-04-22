package com.growthtrace.dashboard.vo;

import com.growthtrace.execution.vo.WeeklyProgressView;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Dashboard 总览聚合视图，对应 Dashboard 页面"7 大区块"。
 * 任一子字段可能为 null（用户数据尚不完整），前端按需隐藏卡片。
 */
@Data
@Builder
public class DashboardOverviewView {

    /** 画像完整度；用户未建档时为 null。 */
    private Integer profileCompleteness;

    private PrimaryTargetView primaryTarget;

    private WeeklyProgressView weeklyTask;

    private LatestDiagnosisDigest latestDiagnosis;

    private List<JournalDigest> recentJournals;

    private List<GrowthCurvePoint> growthCurve;

    private List<HeatmapPoint> heatmap;
}
