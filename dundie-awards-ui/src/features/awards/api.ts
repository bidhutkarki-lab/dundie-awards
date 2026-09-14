import { fetchPage, postJson, type PageResponse } from '../../api/client'
import type { DundieAward, LeaderboardEntry } from './types'

export function fetchAwards(page: number, size: number): Promise<PageResponse<DundieAward>> {
  return fetchPage<DundieAward>('/dundie-awards', page, size)
}

export function fetchLeaderboard(
  page: number,
  size: number,
): Promise<PageResponse<LeaderboardEntry>> {
  return fetchPage<LeaderboardEntry>('/dundie-awards/leaderboard', page, size)
}

export function giveAward(input: {
  recipientId: number
  giverId: number
}): Promise<DundieAward> {
  return postJson<DundieAward>('/dundie-awards', input)
}
