import {
  deleteResource,
  fetchPage,
  postJson,
  putJson,
  type PageResponse,
} from '../../api/client'
import type { Employee } from './types'

export type EmployeeInput = {
  firstName: string
  lastName: string
  organizationId: number
}

export function fetchEmployees(
  page: number,
  size: number,
  search = '',
): Promise<PageResponse<Employee>> {
  return fetchPage<Employee>('/employees', page, size, { search })
}

// picker-sized slice of a server-side search, optionally scoped to one organization
export async function searchEmployees(term: string, organizationId?: number): Promise<Employee[]> {
  const page = await fetchPage<Employee>('/employees', 0, 20, {
    search: term,
    organizationId,
  })
  return page.content
}

export function createEmployee(input: EmployeeInput): Promise<Employee> {
  return postJson<Employee>('/employees', input)
}

export function updateEmployee(id: number, input: EmployeeInput): Promise<Employee> {
  return putJson<Employee>(`/employees/${id}`, input)
}

export function deleteEmployee(id: number): Promise<unknown> {
  return deleteResource(`/employees/${id}`)
}
