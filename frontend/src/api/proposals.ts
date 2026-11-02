import client from './client';
import axios from 'axios';
import type { Proposal, ProposalRequest, ProposalTemplate, PublicProposal, PageResponse } from '../types';

export interface ProposalFilters {
  status?: string;
  clientId?: string;
  search?: string;
  page?: number;
  size?: number;
}

export const proposalsApi = {
  getProposals: async (filters: ProposalFilters = {}) => {
    const response = await client.get<PageResponse<Proposal>>('/api/v1/proposals', { params: filters });
    return response.data;
  },

  getProposal: async (id: string) => {
    const response = await client.get<Proposal>(`/api/v1/proposals/${id}`);
    return response.data;
  },

  createProposal: async (data: ProposalRequest) => {
    const response = await client.post<Proposal>('/api/v1/proposals', { ...data, version: data.version ?? 0 });
    return response.data;
  },

  editProposal: async (id: string, data: ProposalRequest) => {
    const response = await client.put<Proposal>(`/api/v1/proposals/${id}`, data);
    return response.data;
  },

  deleteProposal: async (id: string) => {
    await client.delete(`/api/v1/proposals/${id}`);
  },

  sendProposal: async (id: string) => {
    const response = await client.post<Proposal>(`/api/v1/proposals/${id}/send`);
    return response.data;
  },

  convertToInvoice: async (id: string) => {
    const response = await client.post(`/api/v1/proposals/${id}/convert-to-invoice`);
    return response.data;
  },

  downloadPdf: async (id: string) => {
    const response = await client.get(`/api/v1/proposals/${id}/pdf`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `proposal-${id}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  getTemplates: async () => {
    const response = await client.get<ProposalTemplate[]>('/api/v1/proposal-templates');
    return response.data;
  },

  getPublicProposal: async (token: string) => {
    const response = await axios.get<PublicProposal>(`/api/v1/public/proposals/${token}`);
    return response.data;
  },

  acceptProposal: async (token: string, signatureData: string) => {
    const response = await axios.post<PublicProposal>(`/api/v1/public/proposals/${token}/accept`, { signatureData });
    return response.data;
  },

  declineProposal: async (token: string, reason?: string) => {
    const response = await axios.post<PublicProposal>(`/api/v1/public/proposals/${token}/decline`, { reason });
    return response.data;
  },
};
