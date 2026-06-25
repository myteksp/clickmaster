import api from './client';
import type {
  User,
  Site,
  Campaign,
  CampaignRun,
  Scenario,
  SimulationStats,
} from './types';

export const authApi = {
  register: (email: string, password: string, name: string) =>
    api.post<User>('/auth/register', { email, password, name }),

  login: (email: string, password: string) =>
    api.post<User>('/auth/login', { email, password }),
};

export const sitesApi = {
  list: () => api.get<Site[]>('/sites'),
  get: (id: string) => api.get<Site>(`/sites/${id}`),
  create: (data: Partial<Site>) => api.post<Site>('/sites', data),
  update: (id: string, data: Partial<Site>) => api.put<Site>(`/sites/${id}`, data),
  delete: (id: string) => api.delete(`/sites/${id}`),
};

export const campaignsApi = {
  list: () => api.get<Campaign[]>('/campaigns'),
  get: (id: string) => api.get<Campaign>(`/campaigns/${id}`),
  create: (data: Record<string, unknown>) => api.post<Campaign>('/campaigns', data),
  update: (id: string, data: Record<string, unknown>) => api.put<Campaign>(`/campaigns/${id}`, data),
  delete: (id: string) => api.delete(`/campaigns/${id}`),
  start: (id: string) => api.post<CampaignRun>(`/campaigns/${id}/start`),
  stop: (id: string) => api.post<CampaignRun>(`/campaigns/${id}/stop`),
  pause: (id: string) => api.post(`/campaigns/${id}/pause`),
  resume: (id: string) => api.post(`/campaigns/${id}/resume`),
  runs: (id: string) => api.get<CampaignRun[]>(`/campaigns/${id}/runs`),
  stats: (id: string) => api.get<SimulationStats>(`/campaigns/${id}/stats`),
};

export const scenariosApi = {
  list: () => api.get<Scenario[]>('/scenarios'),
  get: (id: string) => api.get<Scenario>(`/scenarios/${id}`),
  create: (data: Record<string, unknown>) => api.post<Scenario>('/scenarios', data),
  update: (id: string, data: Record<string, unknown>) => api.put<Scenario>(`/scenarios/${id}`, data),
  delete: (id: string) => api.delete(`/scenarios/${id}`),
};

export const asocksApi = {
  countries: () => api.get<{ code: string; name: string }[]>('/asocks/countries'),
  balance: () => api.get<{ balance: number }>('/asocks/balance'),
};
