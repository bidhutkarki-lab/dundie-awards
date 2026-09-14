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
import EmployeeFormDialog from './EmployeeFormDialog'
import {
  createEmployee,
  deleteEmployee,
  fetchEmployees,
  updateEmployee,
  type EmployeeInput,
} from './api'
import type { Employee } from './types'

function toInput(employee: Employee): EmployeeInput | undefined {
  // an employee with no organization cannot be represented by the form, which requires one
  return employee.organizationId === null
    ? undefined
    : {
        firstName: employee.firstName,
        lastName: employee.lastName,
        organizationId: employee.organizationId,
      }
}

export default function EmployeesPage() {
  const queryClient = useQueryClient()
  const resource = usePagedQuery('employees', fetchEmployees)
  const [addOpen, setAddOpen] = useState(false)
  const [pendingEdit, setPendingEdit] = useState<Employee | null>(null)
  const [pendingDelete, setPendingDelete] = useState<Employee | null>(null)

  // every write also records an activity row
  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['employees'] }),
      queryClient.invalidateQueries({ queryKey: ['activities'] }),
    ])

  const addMutation = useMutation({
    mutationFn: createEmployee,
    onSuccess: async () => {
      await refresh()
      setAddOpen(false)
    },
  })

  const editMutation = useMutation({
    mutationFn: ({ id, input }: { id: number; input: EmployeeInput }) => updateEmployee(id, input),
    onSuccess: async () => {
      await refresh()
      setPendingEdit(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteEmployee,
    onSuccess: async () => {
      await refresh()
      setPendingDelete(null)
    },
  })

  const columns: Column<Employee>[] = [
    { key: 'id', label: 'ID', width: 80, render: (employee) => employee.id },
    { key: 'firstName', label: 'First name', render: (employee) => employee.firstName },
    { key: 'lastName', label: 'Last name', render: (employee) => employee.lastName },
    {
      key: 'organization',
      label: 'Organization',
      render: (employee) => employee.organizationName ?? '—',
    },
    {
      key: 'dundieAwards',
      label: 'Dundie awards',
      width: 140,
      render: (employee) => employee.dundieAwards,
    },
    {
      key: 'actions',
      label: '',
      width: 120,
      render: (employee) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="Edit employee">
            <IconButton
              aria-label={`Edit ${employee.firstName} ${employee.lastName}`}
              size="small"
              onClick={() => {
                editMutation.reset()
                setPendingEdit(employee)
              }}
            >
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Delete employee">
            <IconButton
              aria-label={`Delete ${employee.firstName} ${employee.lastName}`}
              size="small"
              onClick={() => {
                deleteMutation.reset()
                setPendingDelete(employee)
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
          Add employee
        </Button>
      </Box>

      <PagedTable
        label="Employees"
        columns={columns}
        rows={resource.rows}
        getRowKey={(employee) => employee.id}
        emptyMessage="No employees yet"
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
        <EmployeeFormDialog
          title="Add employee"
          submitLabel="Add"
          saving={addMutation.isPending}
          error={addMutation.error ? addMutation.error.message : null}
          onClose={() => setAddOpen(false)}
          onSubmit={(input) => addMutation.mutate(input)}
        />
      )}

      {pendingEdit && (
        <EmployeeFormDialog
          title="Edit employee"
          submitLabel="Save"
          initialValue={toInput(pendingEdit)}
          saving={editMutation.isPending}
          error={editMutation.error ? editMutation.error.message : null}
          onClose={() => setPendingEdit(null)}
          onSubmit={(input) => editMutation.mutate({ id: pendingEdit.id, input })}
        />
      )}

      {pendingDelete && (
        <ConfirmDialog
          title="Delete employee"
          message={`Delete ${pendingDelete.firstName} ${pendingDelete.lastName}? They are removed from the directory, but their award history is kept.`}
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
