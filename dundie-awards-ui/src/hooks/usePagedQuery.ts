import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { PageResponse } from '../api/client'

type PagedQuery<T> = {
  rows: T[]
  totalElements: number
  page: number
  rowsPerPage: number
  loading: boolean
  error: string | null
  setPage: (page: number) => void
  setRowsPerPage: (rowsPerPage: number) => void
}

export function usePagedQuery<T>(
  resource: string,
  load: (page: number, size: number) => Promise<PageResponse<T>>,
  initialRowsPerPage = 10,
): PagedQuery<T> {
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(initialRowsPerPage)

  const query = useQuery({
    queryKey: [resource, page, rowsPerPage],
    queryFn: () => load(page, rowsPerPage),
    // hold the previous page on screen while the next one loads, instead of flashing a spinner
    placeholderData: keepPreviousData,
  })

  return {
    rows: query.data?.content ?? [],
    totalElements: query.data?.totalElements ?? 0,
    page,
    rowsPerPage,
    loading: query.isPending,
    error: query.error ? query.error.message : null,
    setPage,
    setRowsPerPage: (next: number) => {
      setRowsPerPage(next)
      setPage(0)
    },
  }
}
