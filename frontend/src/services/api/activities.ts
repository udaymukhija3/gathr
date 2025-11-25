import {
  Activity,
  ActivityDetail,
  CreateActivityRequest,
} from '../../types';
import { apiRequest, MOCK_MODE } from './client';
import { MOCK_ACTIVITIES, MOCK_HUBS, MOCK_MESSAGES, distanceBetween } from './mockData';

export const activitiesApi = {
  getByHub: async (hubId: number, date?: string): Promise<Activity[]> => {
    if (MOCK_MODE) {
      return MOCK_ACTIVITIES.filter(a => a.hubId === hubId);
    }
    const params = new URLSearchParams({ hub_id: hubId.toString() });
    if (date) {
      params.append('date', date);
    }
    return apiRequest<Activity[]>(`/activities?${params.toString()}`);
  },

  getNearby: async (latitude: number, longitude: number, radiusKm = 5, date?: string): Promise<Activity[]> => {
    if (MOCK_MODE) {
      return MOCK_ACTIVITIES.map(activity => {
        if (activity.latitude == null || activity.longitude == null) {
          return { ...activity, distanceKm: Number.POSITIVE_INFINITY };
        }
        const distanceKm = distanceBetween(activity.latitude, activity.longitude, latitude, longitude);
        return { ...activity, distanceKm };
      }).filter(a => (a.distanceKm ?? Number.POSITIVE_INFINITY) <= radiusKm);
    }
    const params = new URLSearchParams({
      latitude: latitude.toString(),
      longitude: longitude.toString(),
      radiusKm: radiusKm.toString(),
    });
    if (date) {
      params.append('date', date);
    }
    return apiRequest<Activity[]>(`/activities?${params.toString()}`);
  },

  getById: async (id: number): Promise<ActivityDetail> => {
    if (MOCK_MODE) {
      const activity = MOCK_ACTIVITIES.find(a => a.id === id);
      if (!activity) {
        throw new Error('Activity not found');
      }
      return {
        ...activity,
        participants: [
          { anonId: 'user_1', mutualsCount: 2, revealed: false, userId: 1 },
          { anonId: 'user_2', mutualsCount: 1, revealed: false, userId: 2 },
          { anonId: 'user_3', mutualsCount: 0, revealed: true, name: 'John', userId: 3 },
        ],
        messagesCount: MOCK_MESSAGES[id]?.length || 0,
      };
    }
    return apiRequest<ActivityDetail>(`/activities/${id}`);
  },

  create: async (data: CreateActivityRequest): Promise<Activity> => {
    if (MOCK_MODE) {
      const hub = data.hubId ? MOCK_HUBS.find(h => h.id === data.hubId) : null;
      const newActivity: Activity = {
        id: Math.floor(Math.random() * 10000),
        title: data.title,
        hubId: hub?.id,
        hubName: hub?.name,
        locationName: data.placeName || hub?.name || data.title,
        locationAddress: data.placeAddress || hub?.area,
        placeId: data.placeId,
        latitude: data.latitude ?? hub?.latitude ?? 0,
        longitude: data.longitude ?? hub?.longitude ?? 0,
        isUserLocation: !hub,
        category: data.category,
        startTime: data.startTime,
        endTime: data.endTime,
        createdBy: 1,
        createdByName: 'Current User',
        interestedCount: 0,
        confirmedCount: 0,
        totalParticipants: 1,
        peopleCount: 1,
        mutualsCount: 0,
        isInviteOnly: data.isInviteOnly ?? false,
        maxMembers: data.maxMembers ?? 4,
      };
      MOCK_ACTIVITIES.push(newActivity);
      return newActivity;
    }
    return apiRequest<Activity>('/activities', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  join: async (id: number, status: 'INTERESTED' | 'CONFIRMED', inviteToken?: string): Promise<void> => {
    if (MOCK_MODE) {
      console.log('Mock: Joining activity', id, 'with status', status, 'token:', inviteToken);
      const activity = MOCK_ACTIVITIES.find(a => a.id === id);
      if (activity) {
        activity.peopleCount = (activity.peopleCount || 0) + 1;
      }
      return;
    }
    const params = new URLSearchParams({ status });
    if (inviteToken) {
      params.append('inviteToken', inviteToken);
    }
    await apiRequest(`/activities/${id}/join?${params.toString()}`, {
      method: 'POST',
    });
  },

  confirm: async (id: number): Promise<void> => {
    if (MOCK_MODE) {
      console.log('Mock: Confirming activity', id);
      return;
    }
    await apiRequest(`/activities/${id}/confirm`, {
      method: 'POST',
    });
  },

  generateInviteToken: async (id: number): Promise<{ token: string; expiresAt: string }> => {
    if (MOCK_MODE) {
      const mockToken = 'mock_token_' + Date.now();
      return {
        token: mockToken,
        expiresAt: new Date(Date.now() + 48 * 60 * 60 * 1000).toISOString(),
      };
    }
    return apiRequest<{ token: string; expiresAt: string }>(`/activities/${id}/invite-token`, {
      method: 'POST',
    });
  },

  invite: async (id: number, phone: string): Promise<void> => {
    if (MOCK_MODE) {
      console.log('Mock: Inviting', phone, 'to activity', id);
      return;
    }
    await apiRequest(`/activities/${id}/invite`, {
      method: 'POST',
      body: JSON.stringify({ phone }),
    });
  },
};

