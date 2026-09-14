import { fetchPage, type PageResponse } from '../../api/client'
import type { Activity } from './types'

export function fetchActivities(page: number, size: number): Promise<PageResponse<Activity>> {
  return fetchPage<Activity>('/activities', page, size)
}
