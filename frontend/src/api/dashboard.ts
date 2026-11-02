import client from './client';

export interface DashboardSummary {
  totalRevenue: number;
  totalOutstanding: number;
  totalOverdueThisMonth: number;
  activeClientsCount: number;
  conversionRate: number;
}

export interface RevenueDataPoint {
  month: string;
  revenue: number;
}

export interface ActivityEvent {
  eventType: string;
  description: string;
  timestamp: string;
  linkTarget: string;
}

export const dashboardApi = {
  getDashboardSummary: async () => {
    const response = await client.get<DashboardSummary>('/api/v1/dashboard/summary');
    return response.data;
  },

  getRevenueChart: async () => {
    const response = await client.get<RevenueDataPoint[]>('/api/v1/dashboard/revenue-chart');
    return response.data;
  },

  getActivityFeed: async () => {
    const response = await client.get<ActivityEvent[]>('/api/v1/dashboard/activity');
    return response.data;
  },
};
