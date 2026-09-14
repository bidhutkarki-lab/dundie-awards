import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Stack from '@mui/material/Stack'
import AsyncAutocomplete from '../../components/AsyncAutocomplete'
import { searchEmployees } from '../employees/api'
import type { Employee } from '../employees/types'

type Props = {
  saving: boolean
  error: string | null
  onClose: () => void
  onSubmit: (input: { recipientId: number; giverId: number }) => void
}

const fullName = (employee: Employee) =>
  employee.organizationName
    ? `${employee.firstName} ${employee.lastName} — ${employee.organizationName}`
    : `${employee.firstName} ${employee.lastName}`

export default function GiveAwardDialog({ saving, error, onClose, onSubmit }: Props) {
  const [recipient, setRecipient] = useState<Employee | null>(null)
  const [giver, setGiver] = useState<Employee | null>(null)

  const recipientOrganizationId = recipient?.organizationId ?? undefined
  const complete = recipient !== null && giver !== null

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <form
        onSubmit={(event) => {
          event.preventDefault()
          if (complete) onSubmit({ recipientId: recipient.id, giverId: giver.id })
        }}
      >
        <DialogTitle>Give a Dundie Award</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <AsyncAutocomplete<Employee>
              label="Recipient"
              queryKey="employee-search"
              search={(term) => searchEmployees(term)}
              value={recipient}
              onChange={(next) => {
                setRecipient(next)
                // a giver from the previous recipient's organization would be rejected
                setGiver(null)
              }}
              getOptionLabel={fullName}
              isOptionEqualToValue={(option, current) => option.id === current.id}
              disabled={saving}
              required
              helperText="Type to search employees"
            />

            <AsyncAutocomplete<Employee>
              label="Given by"
              // scoping the key by organization keeps one recipient's colleagues out of another's cache
              queryKey={`colleague-search-${recipientOrganizationId ?? 'none'}`}
              search={async (term) => {
                if (recipientOrganizationId === undefined || !recipient) return []
                const colleagues = await searchEmployees(term, recipientOrganizationId)
                // the backend rejects self-awards, so never offer the recipient
                return colleagues.filter((employee) => employee.id !== recipient.id)
              }}
              value={giver}
              onChange={setGiver}
              getOptionLabel={fullName}
              isOptionEqualToValue={(option, current) => option.id === current.id}
              disabled={saving || recipient === null}
              required
              helperText={
                recipient
                  ? 'Only colleagues in the same organization can give an award'
                  : 'Pick a recipient first'
              }
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={saving || !complete}>
            Give award
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
