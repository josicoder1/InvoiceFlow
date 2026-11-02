import axios from 'axios';
import type { PublicInvoice } from '../types';

export const publicInvoicesApi = {
  getInvoice: async (token: string) => {
    const response = await axios.get<PublicInvoice>(`/api/v1/public/invoices/${token}`);
    return response.data;
  },

  payInvoice: async (token: string) => {
    const response = await axios.post<{ checkoutUrl: string; sessionId: string }>(
      `/api/v1/public/invoices/${token}/pay`,
    );
    return response.data;
  },

  confirmPayment: async (token: string, txRef: string) => {
    const response = await axios.post<PublicInvoice>(
      `/api/v1/public/invoices/${token}/confirm-payment`,
      { txRef },
    );
    return response.data;
  },
};
