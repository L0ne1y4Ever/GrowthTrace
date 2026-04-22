import request from '@/utils/request'
import type {
  DashboardOverviewView,
  GrowthCurvePoint,
  HeatmapPoint
} from '@/types/dashboard'

export function fetchOverview(): Promise<DashboardOverviewView> {
  return request.get('/dashboard/overview') as unknown as Promise<DashboardOverviewView>
}

export function fetchHeatmap(params: { windowDays?: number } = {}): Promise<HeatmapPoint[]> {
  return request.get('/dashboard/heatmap', { params }) as unknown as Promise<HeatmapPoint[]>
}

export function fetchGrowthCurve(params: { limit?: number } = {}): Promise<GrowthCurvePoint[]> {
  return request.get('/dashboard/growth-curve', { params }) as unknown as Promise<GrowthCurvePoint[]>
}
