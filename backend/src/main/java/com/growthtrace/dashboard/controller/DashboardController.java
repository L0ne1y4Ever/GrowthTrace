package com.growthtrace.dashboard.controller;

import com.growthtrace.common.result.R;
import com.growthtrace.common.security.SecurityUserDetails;
import com.growthtrace.dashboard.service.DashboardService;
import com.growthtrace.dashboard.vo.DashboardOverviewView;
import com.growthtrace.dashboard.vo.GrowthCurvePoint;
import com.growthtrace.dashboard.vo.HeatmapPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public R<DashboardOverviewView> overview() {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(dashboardService.overview(userId));
    }

    @GetMapping("/heatmap")
    public R<List<HeatmapPoint>> heatmap(
            @RequestParam(value = "windowDays", defaultValue = "90") int windowDays) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(dashboardService.heatmap(userId, windowDays));
    }

    @GetMapping("/growth-curve")
    public R<List<GrowthCurvePoint>> growthCurve(
            @RequestParam(value = "limit", defaultValue = "30") int limit) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(dashboardService.growthCurve(userId, limit));
    }
}
