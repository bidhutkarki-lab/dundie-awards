export type DundieAward = {
  id: number
  recipientId: number
  recipientName: string
  giverId: number
  giverName: string
  organizationId: number
  organizationName: string
  awardedAt: string
}

export type LeaderboardEntry = {
  recipientId: number
  recipientName: string
  organizationName: string | null
  awardCount: number
}
