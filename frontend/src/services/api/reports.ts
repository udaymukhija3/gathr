import { apiRequest, MOCK_MODE } from './client';

export const reportsApi = {
  create: async (targetUserId: number, activityId: number | undefined, reason: string): Promise<void> => {
    if (MOCK_MODE) {
      console.log('Mock: Creating report', { targetUserId, activityId, reason });
      return;
    }
    await apiRequest('/reports', {
      method: 'POST',
      body: JSON.stringify({ targetUserId, activityId, reason }),
    });
  },
};

