import type { ApiResponse } from '../../types/api';

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

function buildUrl(path: string) {
  if (!apiBaseUrl) {
    return path;
  }

  return `${apiBaseUrl}${path}`;
}

async function parseResponse<T>(response: Response): Promise<ApiResponse<T>> {
  const rawBody = await response.text();

  if (!rawBody) {
    throw new Error(`Request failed with status ${response.status}: empty response body`);
  }

  let payload: ApiResponse<T>;

  try {
    payload = JSON.parse(rawBody) as ApiResponse<T>;
  } catch {
    const contentType = response.headers.get('content-type') ?? 'unknown content-type';
    throw new Error(`Request failed with status ${response.status}: expected JSON but received ${contentType}`);
  }

  if (!response.ok || !payload.success) {
    const details = payload.error?.details?.join(', ');
    throw new Error(details || payload.error?.message || payload.message || 'Request failed');
  }

  return payload;
}

export const apiClient = {
  async post<TResponse, TBody>(url: string, body: TBody): Promise<ApiResponse<TResponse>> {
    const response = await fetch(buildUrl(url), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    return parseResponse<TResponse>(response);
  },
};
