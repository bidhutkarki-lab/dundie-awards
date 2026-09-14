import PagedTable, { type Column } from '../../components/PagedTable'
import { usePagedQuery } from '../../hooks/usePagedQuery'
import { fetchActivities } from './api'
import type { Activity } from './types'

const columns: Column<Activity>[] = [
  { key: 'id', label: 'ID', width: 80, render: (activity) => activity.id },
  {
    key: 'occurredAt',
    label: 'Occurred at',
    width: 220,
    // the backend sends a LocalDateTime with no zone, so render it as-is in local terms
    render: (activity) => new Date(activity.occurredAt).toLocaleString(),
  },
  { key: 'event', label: 'Event', render: (activity) => activity.event },
]

export default function ActivitiesPage() {
  const resource = usePagedQuery('activities', fetchActivities)

  return (
    <PagedTable
      label="Activities"
      columns={columns}
      rows={resource.rows}
      getRowKey={(activity) => activity.id}
      emptyMessage="No activity recorded yet"
      loading={resource.loading}
      error={resource.error}
      totalElements={resource.totalElements}
      page={resource.page}
      rowsPerPage={resource.rowsPerPage}
      onPageChange={resource.setPage}
      onRowsPerPageChange={resource.setRowsPerPage}
    />
  )
}
