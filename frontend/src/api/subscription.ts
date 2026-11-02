import client from './client';

export interface SubscriptionStatus {
  plan: 'FREE' | 'STARTER' | 'PRO';
  invoicesThisMonth: number;
  totalClients: number;
}

export const subscriptionApi = {
  getSubscriptionStatus: async () => {
    const response = await client.get<SubscriptionStatus>('/api/v1/subscription');
    return response.data;
  },

  upgradePlan: async (plan: 'STARTER' | 'PRO') => {
    const response = await client.post<SubscriptionStatus>('/api/v1/subscription/upgrade', { plan });
    return response.data;
  },

  cancelSubscription: async () => {
    const response = await client.post<SubscriptionStatus>('/api/v1/subscription/cancel');
    return response.data;
  },
};
