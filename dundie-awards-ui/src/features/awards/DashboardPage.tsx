import { useState } from 'react'
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import PagedTable, { type Column } from '../../components/PagedTable'
import { usePagedQuery } from '../../hooks/usePagedQuery'
import GiveAwardDialog from './GiveAwardDialog'
import { fetchLeaderboard, giveAward } from './api'
import type { LeaderboardEntry } from './types'

const columns: Column<LeaderboardEntry>[] = [
  { key: 'recipient', label: 'Recipient', render: (entry) => entry.recipientName },
  {
    key: 'organization',
    label: 'Organization',
    render: (entry) => entry.organizationName ?? '—',
  },
  {
    key: 'awardCount',
    label: 'Dundie awards',
    width: 160,
    render: (entry) => entry.awardCount,
  },
]

export default function DashboardPage() {
  const queryClient = useQueryClient()
  const resource = usePagedQuery('leaderboard', fetchLeaderboard)
  const [giveOpen, setGiveOpen] = useState(false)

  const giveMutation = useMutation({
    mutationFn: giveAward,
    onSuccess: async () => {
      // an award changes the leaderboard, the award log, the employee counts and the activity feed
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['leaderboard'] }),
        queryClient.invalidateQueries({ queryKey: ['awards'] }),
        queryClient.invalidateQueries({ queryKey: ['employees'] }),
        queryClient.invalidateQueries({ queryKey: ['employee-search'] }),
        queryClient.invalidateQueries({ queryKey: ['activities'] }),
      ])
      setGiveOpen(false)
    },
  })

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5" component="h1">
          Dundie Awards
        </Typography>
        <Button
          variant="contained"
          startIcon={<EmojiEventsIcon />}
          onClick={() => {
            giveMutation.reset()
            setGiveOpen(true)
          }}
        >
          Give award
        </Button>
      </Box>

      <PagedTable
        label="Dundie award leaderboard"
        columns={columns}
        rows={resource.rows}
        getRowKey={(entry) => entry.recipientId}
        emptyMessage="No awards given yet"
        loading={resource.loading}
        error={resource.error}
        totalElements={resource.totalElements}
        page={resource.page}
        rowsPerPage={resource.rowsPerPage}
        onPageChange={resource.setPage}
        onRowsPerPageChange={resource.setRowsPerPage}
      />

      {giveOpen && (
        <GiveAwardDialog
          saving={giveMutation.isPending}
          error={giveMutation.error ? giveMutation.error.message : null}
          onClose={() => setGiveOpen(false)}
          onSubmit={(input) => giveMutation.mutate(input)}
        />
      )}
    </>
  )
}
