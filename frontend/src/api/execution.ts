import request from '@/utils/request'
import type {
  CheckInPayload,
  CreateTaskPayload,
  TaskStatus,
  TaskView,
  UpdateTaskPayload,
  UpdateTaskStatusPayload,
  WeeklyProgressView
} from '@/types/execution'

export function createTask(payload: CreateTaskPayload): Promise<TaskView> {
  return request.post('/execution/task', payload) as unknown as Promise<TaskView>
}

export interface ListTasksParams {
  status?: TaskStatus
  targetId?: number
}

export function listTasks(params: ListTasksParams = {}): Promise<TaskView[]> {
  return request.get('/execution/task', { params }) as unknown as Promise<TaskView[]>
}

export function fetchTask(id: number): Promise<TaskView> {
  return request.get(`/execution/task/${id}`) as unknown as Promise<TaskView>
}

export function updateTask(id: number, payload: UpdateTaskPayload): Promise<TaskView> {
  return request.put(`/execution/task/${id}`, payload) as unknown as Promise<TaskView>
}

export function updateTaskStatus(
  id: number,
  payload: UpdateTaskStatusPayload
): Promise<TaskView> {
  return request.put(`/execution/task/${id}/status`, payload) as unknown as Promise<TaskView>
}

export function checkInTask(id: number, payload: CheckInPayload = {}): Promise<TaskView> {
  return request.post(`/execution/task/${id}/check-in`, payload) as unknown as Promise<TaskView>
}

export function deleteTask(id: number): Promise<void> {
  return request.delete(`/execution/task/${id}`) as unknown as Promise<void>
}

export function fetchWeeklyProgress(): Promise<WeeklyProgressView> {
  return request.get('/execution/weekly-progress') as unknown as Promise<WeeklyProgressView>
}
