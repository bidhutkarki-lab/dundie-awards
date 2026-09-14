import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import { useQuery } from '@tanstack/react-query'
import AsyncAutocomplete from '../../components/AsyncAutocomplete'
import { fetchOrganization, searchOrganizations } from '../organizations/api'
import type { Organization } from '../organizations/types'
import type { EmployeeInput } from './api'

type Props = {
  title: string
  submitLabel: string
  initialValue?: EmployeeInput
  saving: boolean
  error: string | null
  onClose: () => void
  onSubmit: (input: EmployeeInput) => void
}

export default function EmployeeFormDialog({
  title,
  submitLabel,
  initialValue,
  saving,
  error,
  onClose,
  onSubmit,
}: Props) {
  const [firstName, setFirstName] = useState(initialValue?.firstName ?? '')
  const [lastName, setLastName] = useState(initialValue?.lastName ?? '')
  const [organization, setOrganization] = useState<Organization | null>(null)

  // the current organization may sit outside the first page of search results, so resolve it by id
  const initialOrganizationId = initialValue?.organizationId
  const currentOrganization = useQuery({
    queryKey: ['organization', initialOrganizationId],
    queryFn: () => fetchOrganization(initialOrganizationId!),
    enabled: initialOrganizationId !== undefined,
  })

  const selected = organization ?? currentOrganization.data ?? null
  const trimmedFirst = firstName.trim()
  const trimmedLast = lastName.trim()
  const complete = trimmedFirst !== '' && trimmedLast !== '' && selected !== null
  const unchanged =
    initialValue !== undefined &&
    trimmedFirst === initialValue.firstName &&
    trimmedLast === initialValue.lastName &&
    selected?.id === initialValue.organizationId

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <form
        onSubmit={(event) => {
          event.preventDefault()
          if (complete) {
            onSubmit({
              firstName: trimmedFirst,
              lastName: trimmedLast,
              organizationId: selected.id,
            })
          }
        }}
      >
        <DialogTitle>{title}</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              autoFocus
              fullWidth
              required
              label="First name"
              value={firstName}
              onChange={(event) => setFirstName(event.target.value)}
              disabled={saving}
            />
            <TextField
              fullWidth
              required
              label="Last name"
              value={lastName}
              onChange={(event) => setLastName(event.target.value)}
              disabled={saving}
            />
            <AsyncAutocomplete<Organization>
              label="Organization"
              queryKey="organization-search"
              search={searchOrganizations}
              value={selected}
              onChange={setOrganization}
              getOptionLabel={(option) => option.name}
              isOptionEqualToValue={(option, current) => option.id === current.id}
              disabled={saving}
              required
              helperText="Type to search organizations"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={saving || !complete || unchanged}>
            {submitLabel}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
