import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { api, ApiError, setUnauthorizedHandler } from '../../api/client';

global.fetch = vi.fn();

describe('ApiClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.setToken(null);
  });

  it('makes GET request with auth header when token exists', async () => {
    api.setToken('test-token');
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: async () => JSON.stringify({ data: 'test' }),
    } as Response);

    const result = await api.get('/test');

    expect(fetch).toHaveBeenCalledWith(
      '/api/test',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer test-token',
        }),
      }),
    );
    expect(result).toEqual({ data: 'test' });
  });

  it('throws ApiError on non-2xx response', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: false,
      status: 404,
      text: async () => 'Not found',
    } as Response);

    await expect(api.get('/missing')).rejects.toThrow(ApiError);
  });

  it('parses JSON error body for message', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: false,
      status: 400,
      text: async () => JSON.stringify({ message: 'Validation failed' }),
    } as Response);

    try {
      await api.get('/bad');
    } catch (e) {
      expect(e).toBeInstanceOf(ApiError);
      expect((e as Error).message).toBe('Validation failed');
    }
  });

  it('handles 204 No Content', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      status: 204,
    } as Response);

    const result = await api.delete('/item/1');
    expect(result).toBeUndefined();
  });

  it('triggers unauthorized handler on 401', async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    vi.mocked(fetch).mockResolvedValueOnce({
      ok: false,
      status: 401,
      text: async () => 'Unauthorized',
    } as Response);

    api.setToken('expired-token');
    await expect(api.get('/protected')).rejects.toThrow();
    expect(handler).toHaveBeenCalled();
    expect(api.getToken()).toBeNull();
    setUnauthorizedHandler(null);
  });

  it('adds query params correctly', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: async () => '[]',
    } as Response);

    await api.get('/items', { page: '1', limit: '10' });

    expect(fetch).toHaveBeenCalledWith(
      '/api/items?page=1&limit=10',
      expect.anything(),
    );
  });

  it('sends JSON body on POST', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: async () => JSON.stringify({ id: 1 }),
    } as Response);

    await api.post('/items', { name: 'test' });

    const call = vi.mocked(fetch).mock.calls[0];
    expect(call[1]?.body).toBe(JSON.stringify({ name: 'test' }));
    expect(call[1]?.method).toBe('POST');
  });
});
