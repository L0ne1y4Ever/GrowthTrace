package com.growthtrace.execution.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 本周进度视图（周一到周日）。
 * completedTaskIds 仅含本周被标记 DONE 的任务（completed_at 在周内）。
 */
@Data
@Builder
public class WeeklyProgressView {

    private LocalDate weekStart;
    private LocalDate weekEnd;

    /** 本周到期任务（due_date 落在周内，含 TODO/IN_PROGRESS/DONE/ABANDONED）。 */
    private int dueThisWeek;

    /** 本周到期任务中已 DONE 的数量。 */
    private int doneThisWeek;

    /** 完成率 0–1；无到期任务时为 0。 */
    private double completionRate;

    /** 本周 7 天每天的打卡点数（跨全部任务聚合）。 */
    private List<DayPoint> checkInPerDay;

    @Data
    @Builder
    public static class DayPoint {
        private LocalDate date;
        /** 打卡次数（task check-ins 当天总和）。 */
        private int count;
    }
}
