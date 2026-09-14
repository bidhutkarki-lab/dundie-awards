import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import TextField from '@mui/material/TextField'

type Props = {
  title: string
  submitLabel: string
  initialName?: string
  saving: boolean
  error: string | null
  onClose: () => void
  onSubmit: (name: string) => void
}

export default function OrganizationFormDialog({
  title,
  submitLabel,
  initialName = '',
  saving,
  error,
  onClose,
  onSubmit,
}: Props) {
  const [name, setName] = useState(initialName)
  const trimmed = name.trim()

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <form
        onSubmit={(event) => {
          event.preventDefault()
          if (trimmed) onSubmit(trimmed)
        }}
      >
        <DialogTitle>{title}</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <TextField
            autoFocus
            fullWidth
            required
            margin="dense"
            label="Name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            disabled={saving}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={saving || !trimmed || trimmed === initialName}
          >
            {submitLabel}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
