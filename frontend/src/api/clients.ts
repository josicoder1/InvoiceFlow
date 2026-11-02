import client from './client';
import type { Client, ClientRequest, ClientHistory, PageResponse } from '../types';

export const clientsApi = {
  getClients: async (search?: string, page = 0, size = 10) => {
    const response = await client.get<PageResponse<Client>>('/api/v1/clients', {
      params: { search, page, size },
    });
    return response.data;
  },

  getClient: async (id: string) => {
    const response = await client.get<Client>(`/api/v1/clients/${id}`);
    return response.data;
  },

  addClient: async (data: ClientRequest) => {
    const response = await client.post<Client>('/api/v1/clients', data);
    return response.data;
  },

  editClient: async (id: string, data: ClientRequest) => {
    const response = await client.put<Client>(`/api/v1/clients/${id}`, data);
    return response.data;
  },

  deleteClient: async (id: string) => {
    await client.delete(`/api/v1/clients/${id}`);
  },

  getClientHistory: async (id: string) => {
    const response = await client.get<ClientHistory>(`/api/v1/clients/${id}/history`);
    return response.data;
  },
};
