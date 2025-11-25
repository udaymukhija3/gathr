import {
  TrustScore,
  UpdateProfileRequest,
  UserActivity,
  UserProfile,
} from '../../types';
import { apiRequest, MOCK_MODE } from './client';

export const usersApi = {
  getMe: async (): Promise<UserProfile> => {
    if (MOCK_MODE) {
      return {
        id: 1,
        name: 'Test User',
        phone: '+919876543210',
        bio: 'Love meeting new people!',
        interests: ['SPORTS', 'FOOD'],
        homeHubId: 1,
        homeHubName: 'Cyberhub',
        trustScore: 85,
        activitiesCount: 12,
        onboardingCompleted: true,
      };
    }
    return apiRequest<UserProfile>('/users/me');
  },

  updateMe: async (data: UpdateProfileRequest): Promise<UserProfile> => {
    if (MOCK_MODE) {
      console.log('Mock: Updating profile', data);
      return {
        id: 1,
        name: data.name || 'Test User',
        phone: '+919876543210',
        bio: data.bio || 'Love meeting new people!',
        avatarUrl: data.avatarUrl,
        interests: ['SPORTS', 'FOOD'],
        homeHubId: 1,
        homeHubName: 'Cyberhub',
        trustScore: 85,
        activitiesCount: 12,
        onboardingCompleted: true,
      };
    }
    return apiRequest<UserProfile>('/users/me', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  getTrustScore: async (): Promise<TrustScore> => {
    if (MOCK_MODE) {
      return {
        score: 85,
        activitiesAttended: 10,
        noShows: 1,
        averageRating: 4.5,
        reportsReceived: 0,
      };
    }
    return apiRequest<TrustScore>('/users/me/trust-score');
  },

  getMyActivities: async (): Promise<UserActivity[]> => {
    if (MOCK_MODE) {
      return [
        {
          id: 1,
          title: 'Coffee at Galleria',
          hubId: 2,
          hubName: 'Galleria',
          category: 'FOOD',
          startTime: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
          endTime: new Date(Date.now() + 4 * 60 * 60 * 1000).toISOString(),
          status: 'SCHEDULED',
          participationStatus: 'CONFIRMED',
          isCreator: false,
        },
        {
          id: 2,
          title: 'Badminton Night',
          hubId: 1,
          hubName: 'Cyberhub',
          category: 'SPORTS',
          startTime: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
          endTime: new Date(Date.now() - 22 * 60 * 60 * 1000).toISOString(),
          status: 'COMPLETED',
          participationStatus: 'CONFIRMED',
          isCreator: true,
        },
      ];
    }
    return apiRequest<UserActivity[]>('/users/me/activities');
  },

  updateInterests: async (interests: string[]): Promise<void> => {
    if (MOCK_MODE) {
      console.log('Mock: Updating interests', interests);
      return;
    }
    await apiRequest('/users/me/interests', {
      method: 'PUT',
      body: JSON.stringify({ interests }),
    });
  },
};

