import { ActivityTemplate, CreateTemplateRequest } from '../../types';
import { apiRequest, MOCK_MODE } from './client';

export const templatesApi = {
  getAll: async (type: 'all' | 'system' | 'user' = 'all'): Promise<ActivityTemplate[]> => {
    if (MOCK_MODE) {
      return [
        {
          id: 1,
          name: 'Coffee Meetup',
          title: 'Coffee at Galleria',
          category: 'FOOD',
          durationHours: 1,
          description: 'Casual coffee meetup to chat and connect',
          isSystemTemplate: true,
          isInviteOnly: false,
          maxMembers: 4,
        },
        {
          id: 2,
          name: 'Pickup Basketball',
          title: 'Pickup Basketball Game',
          category: 'SPORTS',
          durationHours: 2,
          description: 'Casual basketball game, all skill levels welcome',
          isSystemTemplate: true,
          isInviteOnly: false,
          maxMembers: 8,
        },
        {
          id: 3,
          name: 'Study Session',
          title: 'Study Group at Library',
          category: 'ART',
          durationHours: 3,
          description: 'Focused study session with like-minded students',
          isSystemTemplate: true,
          isInviteOnly: false,
          maxMembers: 6,
        },
      ];
    }
    return apiRequest<ActivityTemplate[]>(`/templates?type=${type}`);
  },

  create: async (data: CreateTemplateRequest): Promise<ActivityTemplate> => {
    if (MOCK_MODE) {
      const newTemplate: ActivityTemplate = {
        id: Math.floor(Math.random() * 10000),
        ...data,
        isSystemTemplate: false,
        isInviteOnly: data.isInviteOnly || false,
        maxMembers: data.maxMembers || 4,
      };
      return newTemplate;
    }
    return apiRequest<ActivityTemplate>('/templates', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  delete: async (id: number): Promise<void> => {
    if (MOCK_MODE) {
      console.log('Mock: Deleting template', id);
      return;
    }
    await apiRequest(`/templates/${id}`, {
      method: 'DELETE',
    });
  },
};

