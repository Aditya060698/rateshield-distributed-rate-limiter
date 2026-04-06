import type { ApiResponse } from '../../types/api';

async function parseResponse<T>(response: Response): Promise<ApiResponse<T>> {
  const payload = (await response.json()) as ApiResponse<T>;

  if (!response.ok || !payload.success) {
    const details = payload.error?.details?.join(', ');
    throw new Error(details || payload.error?.message || payload.message || 'Request failed');
  }

  return payload;
}

export const apiClient = {
  async post<TResponse, TBody>(url: string, body: TBody): Promise<ApiResponse<TResponse>> {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    return parseResponse<TResponse>(response);
  },
};
