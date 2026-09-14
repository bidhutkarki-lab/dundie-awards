import {
  deleteResource,
  fetchPage,
  getJson,
  postJson,
  putJson,
  type PageResponse,
} from '../../api/client'
import type { Organization } from './types'

export function fetchOrganizations(
  page: number,
  size: number,
  search = '',
): Promise<PageResponse<Organization>> {
  return fetchPage<Organization>('/organizations', page, size, { search })
}

// picker-sized slice of a server-side search, so the list is never truncated silently
export async function searchOrganizations(term: string): Promise<Organization[]> {
  const page = await fetchOrganizations(0, 20, term)
  return page.content
}

export function fetchOrganization(id: number): Promise<Organization> {
  return getJson<Organization>(`/organizations/${id}`)
}

export function createOrganization(name: string): Promise<Organization> {
  return postJson<Organization>('/organizations', { name })
}

export function updateOrganization(id: number, name: string): Promise<Organization> {
  return putJson<Organization>(`/organizations/${id}`, { name })
}

export function deleteOrganization(id: number): Promise<unknown> {
  return deleteResource(`/organizations/${id}`)
}
