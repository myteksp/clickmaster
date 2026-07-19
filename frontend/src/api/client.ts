const API_BASE = '/api';

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: string,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

type UnauthorizedHandler = () => void;
let unauthorizedHandler: UnauthorizedHandler | null = null;

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null) {
  unauthorizedHandler = handler;
}

interface ApiOptions extends RequestInit {
  params?: Record<string, string>;
}

class ApiClient {
  private token: string | null = null;

  setToken(token: string | null) {
    this.token = token;
    if (token) {
      localStorage.setItem('token', token);
    } else {
      localStorage.removeItem('token');
    }
  }

  getToken(): string | null {
    if (!this.token) {
      this.token = localStorage.getItem('token');
    }
    return this.token;
  }

  private parseErrorMessage(body: string, status: number): string {
    try {
      const json = JSON.parse(body);
      if (json.message) return json.message;
      if (json.error) return json.error;
      if (json.errors && Array.isArray(json.errors)) {
        return json.errors.map((e: { field?: string; message?: string }) => `${e.field || ''}: ${e.message || ''}`).join(', ');
      }
      return body || `HTTP ${status}`;
    } catch {
      if (body && body.length < 200) return body;
      return `HTTP ${status}`;
    }
  }

  private async request<T>(endpoint: string, options: ApiOptions = {}): Promise<T> {
    const { params, ...fetchOptions } = options;
    let url = `${API_BASE}${endpoint}`;

    if (params) {
      const searchParams = new URLSearchParams(params);
      url += `?${searchParams.toString()}`;
    }

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...((fetchOptions.headers as Record<string, string>) || {}),
    };

    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(url, { ...fetchOptions, headers });

    if (!response.ok) {
      const body = await response.text();
      const message = this.parseErrorMessage(body, response.status);

      if (response.status === 401) {
        this.setToken(null);
        if (unauthorizedHandler) unauthorizedHandler();
      }

      throw new ApiError(response.status, body, message);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    const text = await response.text();
    if (!text || text.trim().length === 0) {
      return undefined as T;
    }

    try {
      return JSON.parse(text);
    } catch {
      return undefined as T;
    }
  }

  get<T>(endpoint: string, params?: Record<string, string>) {
    return this.request<T>(endpoint, { method: 'GET', params });
  }

  post<T>(endpoint: string, body?: unknown) {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  put<T>(endpoint: string, body?: unknown) {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  patch<T>(endpoint: string, body?: unknown) {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  delete<T>(endpoint: string) {
    return this.request<T>(endpoint, { method: 'DELETE' });
  }
}

export const api = new ApiClient();
export default api;
