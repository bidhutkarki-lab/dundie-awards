import PagedTable, { type Column } from '../../components/PagedTable'
import { usePagedQuery } from '../../hooks/usePagedQuery'
import { fetchAwards } from './api'
import type { DundieAward } from './types'

const columns: Column<DundieAward>[] = [
  { key: 'id', label: 'ID', width: 80, render: (award) => award.id },
  { key: 'recipient', label: 'Recipient', render: (award) => award.recipientName },
  { key: 'giver', label: 'Given by', render: (award) => award.giverName },
  { key: 'organization', label: 'Organization', render: (award) => award.organizationName },
  {
    key: 'awardedAt',
    label: 'Awarded at',
    width: 200,
    render: (award) => new Date(award.awardedAt).toLocaleString(),
  },
]

export default function AwardsPage() {
  const resource = usePagedQuery('awards', fetchAwards)

  return (
    <PagedTable
      label="Dundie award log"
      columns={columns}
      rows={resource.rows}
      getRowKey={(award) => award.id}
      emptyMessage="No awards given yet"
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
