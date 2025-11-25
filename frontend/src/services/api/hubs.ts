import { Hub } from '../../types';
import { apiRequest, MOCK_MODE } from './client';
import { MOCK_HUBS } from './mockData';

export const hubsApi = {
  getAll: async (): Promise<Hub[]> => {
    if (MOCK_MODE) {
      return MOCK_HUBS;
    }
    return apiRequest<Hub[]>('/hubs');
  },

  getNearest: async (latitude: number, longitude: number, limit: number = 5): Promise<Hub[]> => {
    if (MOCK_MODE) {
      return MOCK_HUBS.map((hub, i) => ({
        ...hub,
        distance: (i + 1) * 1.5,
      }));
    }
    return apiRequest<Hub[]>(`/hubs/nearest?latitude=${latitude}&longitude=${longitude}&limit=${limit}`);
  },

  getById: async (hubId: number): Promise<Hub> => {
    if (MOCK_MODE) {
      const hub = MOCK_HUBS.find(h => h.id === hubId);
      if (!hub) throw new Error('Hub not found');
      return hub;
    }
    return apiRequest<Hub>(`/hubs/${hubId}`);
  },
};

