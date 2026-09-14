import type { ReactNode } from 'react'
import Alert from '@mui/material/Alert'
import CircularProgress from '@mui/material/CircularProgress'
import Paper from '@mui/material/Paper'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TablePagination from '@mui/material/TablePagination'
import TableRow from '@mui/material/TableRow'

export type Column<T> = {
  key: string
  label: string
  width?: number
  render: (row: T) => ReactNode
}

type Props<T> = {
  label: string
  columns: Column<T>[]
  rows: T[]
  getRowKey: (row: T) => string | number
  emptyMessage: string
  loading: boolean
  error: string | null
  totalElements: number
  page: number
  rowsPerPage: number
  onPageChange: (page: number) => void
  onRowsPerPageChange: (rowsPerPage: number) => void
}

export default function PagedTable<T>({
  label,
  columns,
  rows,
  getRowKey,
  emptyMessage,
  loading,
  error,
  totalElements,
  page,
  rowsPerPage,
  onPageChange,
  onRowsPerPageChange,
}: Props<T>) {
  return (
    <>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Paper>
        <TableContainer>
          <Table aria-label={label}>
            <TableHead>
              <TableRow>
                {columns.map((column) => (
                  <TableCell key={column.key} width={column.width}>
                    {column.label}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={columns.length} align="center" sx={{ py: 4 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              ) : rows.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={columns.length} align="center" sx={{ py: 4 }}>
                    {emptyMessage}
                  </TableCell>
                </TableRow>
              ) : (
                rows.map((row) => (
                  <TableRow key={getRowKey(row)} hover>
                    {columns.map((column) => (
                      <TableCell key={column.key}>{column.render(row)}</TableCell>
                    ))}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>

        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          rowsPerPage={rowsPerPage}
          rowsPerPageOptions={[5, 10, 25]}
          onPageChange={(_, nextPage) => onPageChange(nextPage)}
          onRowsPerPageChange={(event) => onRowsPerPageChange(Number(event.target.value))}
        />
      </Paper>
    </>
  )
}
