import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'

type Props = {
  title: string
  message: string
  confirmLabel: string
  working: boolean
  error: string | null
  onClose: () => void
  onConfirm: () => void
}

export default function ConfirmDialog({
  title,
  message,
  confirmLabel,
  working,
  error,
  onClose,
  onConfirm,
}: Props) {
  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <DialogContentText>{message}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={working}>
          Cancel
        </Button>
        <Button onClick={onConfirm} color="error" variant="contained" disabled={working}>
          {confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
