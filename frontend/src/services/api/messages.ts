import { Message } from '../../types';
import { apiRequest, MOCK_MODE } from './client';
import { MOCK_MESSAGES } from './mockData';

export const messagesApi = {
  getByActivity: async (activityId: number, since?: string): Promise<Message[]> => {
    if (MOCK_MODE) {
      const messages = MOCK_MESSAGES[activityId] || [];
      if (since) {
        return messages.filter(m => m.createdAt > since);
      }
      return messages;
    }
    const params = since ? `?since=${since}` : '';
    return apiRequest<Message[]>(`/activities/${activityId}/messages${params}`);
  },

  create: async (activityId: number, text: string): Promise<Message> => {
    if (MOCK_MODE) {
      const newMessage: Message = {
        id: Math.floor(Math.random() * 10000),
        activityId,
        userId: 1,
        userName: 'You',
        text,
        createdAt: new Date().toISOString(),
      };
      if (!MOCK_MESSAGES[activityId]) {
        MOCK_MESSAGES[activityId] = [];
      }
      MOCK_MESSAGES[activityId].push(newMessage);
      return newMessage;
    }
    return apiRequest<Message>(`/activities/${activityId}/messages`, {
      method: 'POST',
      body: JSON.stringify({ text }),
    });
  },
};

