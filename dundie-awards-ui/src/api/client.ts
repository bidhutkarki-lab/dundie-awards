export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// the backend reports failures either as {"message": "..."} or as {"<field>": "<error>"}
async function errorMessage(response: Response): Promise<string> {
  try {
    const body: unknown = await response.json()
    if (body && typeof body === 'object') {
      const values = Object.values(body as Record<string, unknown>)
      const message = (body as Record<string, unknown>).message ?? values[0]
      if (typeof message === 'string') return message
    }
  } catch {
    // fall through to the status-code message
  }
  return `Request failed (${response.status})`
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, init)
  if (!response.ok) {
    throw new Error(await errorMessage(response))
  }
  return response.json()
}

export function fetchPage<T>(
  path: string,
  page: number,
  size: number,
  params: Record<string, string | number | undefined> = {},
): Promise<PageResponse<T>> {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') query.set(key, String(value))
  }
  return request<PageResponse<T>>(`${path}?${query.toString()}`)
}

export function getJson<T>(path: string): Promise<T> {
  return request<T>(path)
}

export function postJson<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

export function putJson<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

export function deleteResource(path: string): Promise<unknown> {
  return request<unknown>(path, { method: 'DELETE' })
}
