import { useState } from 'react'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import IconButton from '@mui/material/IconButton'
import Stack from '@mui/material/Stack'
import Tooltip from '@mui/material/Tooltip'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import ConfirmDialog from '../../components/ConfirmDialog'
import PagedTable, { type Column } from '../../components/PagedTable'
import { usePagedQuery } from '../../hooks/usePagedQuery'
import OrganizationFormDialog from './OrganizationFormDialog'
import {
  createOrganization,
  deleteOrganization,
  fetchOrganizations,
  updateOrganization,
} from './api'
import type { Organization } from './types'

export default function OrganizationsPage() {
  const queryClient = useQueryClient()
  const resource = usePagedQuery('organizations', fetchOrganizations)
  const [addOpen, setAddOpen] = useState(false)
  const [pendingEdit, setPendingEdit] = useState<Organization | null>(null)
  const [pendingDelete, setPendingDelete] = useState<Organization | null>(null)

  // every write also records an activity row
  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['organizations'] }),
      queryClient.invalidateQueries({ queryKey: ['activities'] }),
    ])

  const addMutation = useMutation({
    mutationFn: createOrganization,
    onSuccess: async () => {
      await refresh()
      setAddOpen(false)
    },
  })

  const editMutation = useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) => updateOrganization(id, name),
    onSuccess: async () => {
      await refresh()
      setPendingEdit(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteOrganization,
    onSuccess: async () => {
      await refresh()
      setPendingDelete(null)
    },
  })

  const columns: Column<Organization>[] = [
    { key: 'id', label: 'ID', width: 120, render: (organization) => organization.id },
    { key: 'name', label: 'Name', render: (organization) => organization.name },
    {
      key: 'actions',
      label: '',
      width: 120,
      render: (organization) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="Edit organization">
            <IconButton
              aria-label={`Edit ${organization.name}`}
              size="small"
              onClick={() => {
                editMutation.reset()
                setPendingEdit(organization)
              }}
            >
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Delete organization">
            <IconButton
              aria-label={`Delete ${organization.name}`}
              size="small"
              onClick={() => {
                deleteMutation.reset()
                setPendingDelete(organization)
              }}
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ]

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => {
            addMutation.reset()
            setAddOpen(true)
          }}
        >
          Add organization
        </Button>
      </Box>

      <PagedTable
        label="Organizations"
        columns={columns}
        rows={resource.rows}
        getRowKey={(organization) => organization.id}
        emptyMessage="No organizations yet"
        loading={resource.loading}
        error={resource.error}
        totalElements={resource.totalElements}
        page={resource.page}
        rowsPerPage={resource.rowsPerPage}
        onPageChange={resource.setPage}
        onRowsPerPageChange={resource.setRowsPerPage}
      />

      {/* dialogs are mounted only while active, so their form state never leaks between openings */}
      {addOpen && (
        <OrganizationFormDialog
          title="Add organization"
          submitLabel="Add"
          saving={addMutation.isPending}
          error={addMutation.error ? addMutation.error.message : null}
          onClose={() => setAddOpen(false)}
          onSubmit={(name) => addMutation.mutate(name)}
        />
      )}

      {pendingEdit && (
        <OrganizationFormDialog
          title="Edit organization"
          submitLabel="Save"
          initialName={pendingEdit.name}
          saving={editMutation.isPending}
          error={editMutation.error ? editMutation.error.message : null}
          onClose={() => setPendingEdit(null)}
          onSubmit={(name) => editMutation.mutate({ id: pendingEdit.id, name })}
        />
      )}

      {pendingDelete && (
        <ConfirmDialog
          title="Delete organization"
          message={`Delete "${pendingDelete.name}"? This cannot be undone.`}
          confirmLabel="Delete"
          working={deleteMutation.isPending}
          error={deleteMutation.error ? deleteMutation.error.message : null}
          onClose={() => setPendingDelete(null)}
          onConfirm={() => deleteMutation.mutate(pendingDelete.id)}
        />
      )}
    </>
  )
}
