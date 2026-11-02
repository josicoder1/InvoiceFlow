import client from './client';
import type { Invoice, InvoiceRequest, PageResponse } from '../types';

export interface InvoiceFilters {
  status?: string;
  clientId?: string;
  startDate?: string;
  endDate?: string;
  search?: string;
  page?: number;
  size?: number;
}

export const invoicesApi = {
  getInvoices: async (filters: InvoiceFilters = {}) => {
    const response = await client.get<PageResponse<Invoice>>('/api/v1/invoices', {
      params: filters,
    });
    return response.data;
  },

  getInvoice: async (id: string) => {
    const response = await client.get<Invoice>(`/api/v1/invoices/${id}`);
    return response.data;
  },

  createInvoice: async (data: InvoiceRequest) => {
    const response = await client.post<Invoice>('/api/v1/invoices', data);
    return response.data;
  },

  editInvoice: async (id: string, data: InvoiceRequest) => {
    const response = await client.put<Invoice>(`/api/v1/invoices/${id}`, data);
    return response.data;
  },

  deleteInvoice: async (id: string) => {
    await client.delete(`/api/v1/invoices/${id}`);
  },

  duplicateInvoice: async (id: string) => {
    const response = await client.post<Invoice>(`/api/v1/invoices/${id}/duplicate`);
    return response.data;
  },

  markInvoicePaid: async (id: string) => {
    const response = await client.post<Invoice>(`/api/v1/invoices/${id}/mark-paid`);
    return response.data;
  },

  markInvoiceSent: async (id: string) => {
    const response = await client.post<Invoice>(`/api/v1/invoices/${id}/send`);
    return response.data;
  },

  cancelInvoice: async (id: string) => {
    const response = await client.post<Invoice>(`/api/v1/invoices/${id}/cancel`);
    return response.data;
  },

  downloadPdf: async (id: string) => {
    const response = await client.get(`/api/v1/invoices/${id}/pdf`, {
      responseType: 'blob',
    });
    const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `invoice-${id}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  previewInvoice: async (id: string): Promise<string> => {
    const response = await client.get<string>(`/api/v1/invoices/${id}/preview`);
    return response.data;
  },
};
