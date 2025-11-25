import { apiRequest, MOCK_MODE } from './client';

export const contactsApi = {
  upload: async (hashes: string[]): Promise<{ mutualsCount: number }> => {
    if (MOCK_MODE) {
      return { mutualsCount: Math.floor(Math.random() * 5) };
    }
    return apiRequest<{ mutualsCount: number }>('/contacts/upload', {
      method: 'POST',
      body: JSON.stringify({ hashes }),
    });
  },
};

