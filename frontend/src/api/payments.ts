import client from './client';

export const paymentsApi = {
  connectChapa: async (chapaMerchantKey: string) => {
    const response = await client.post<void>('/api/v1/payments/chapa/connect', { chapaMerchantKey });
    return response.data;
  },

  getNotificationSettings: async () => {
    const response = await client.get<NotificationSettings>('/api/v1/users/me/notifications');
    return response.data;
  },

  updateNotificationSettings: async (settings: NotificationSettings) => {
    const response = await client.put<NotificationSettings>('/api/v1/users/me/notifications', settings);
    return response.data;
  },
};

export interface NotificationSettings {
  remindersEnabled: boolean;
  reminderDaysBeforeDue: number;
  reminderDaysAfterDue: number;
}
