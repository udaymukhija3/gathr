import * as SecureStore from 'expo-secure-store';
import { Activity, ActivityDetail, ActivityTemplate, AuthResponse, CreateActivityRequest, CreateTemplateRequest, Hub, Message, User } from '../types';

// Mock mode: Set to true to use mock data, false to use real API
// Can be toggled via EXPO_PUBLIC_MOCK_MODE environment variable
// PRODUCTION: Defaults to false (real API mode)
// DEVELOPMENT: Set EXPO_PUBLIC_MOCK_MODE=true to use mock data
const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080';
const MOCK_MODE = process.env.EXPO_PUBLIC_MOCK_MODE === 'true'; // Default to false (production mode)

// Mock data
const MOCK_HUBS: Hub[] = [
  { id: 1, name: 'Cyberhub', area: 'Cyber City', description: 'A bustling hub with restaurants, cafes, and entertainment venues' },
  { id: 2, name: 'Galleria', area: 'DLF Galleria', description: 'Shopping and dining destination in the heart of Gurgaon' },
  { id: 3, name: '32nd Avenue', area: 'Sector 32', description: 'Popular food and nightlife street' },
];

const MOCK_ACTIVITIES: Activity[] = [
  {
    id: 1,
    title: 'Coffee at Galleria',
    hubId: 2,
    hubName: 'Galleria',
    category: 'FOOD',
    startTime: '2025-11-12T18:30:00+05:30',
    endTime: '2025-11-12T20:00:00+05:30',
    createdBy: 1,
    createdByName: 'Test User',
    peopleCount: 7,
    mutualsCount: 2,
    isInviteOnly: true,
  },
  {
    id: 2,
    title: 'Pickleball Session',
    hubId: 1,
    hubName: 'Cyberhub',
    category: 'SPORTS',
    startTime: '2025-11-12T19:00:00+05:30',
    endTime: '2025-11-12T21:00:00+05:30',
    createdBy: 1,
    createdByName: 'Test User',
    peopleCount: 5,
    mutualsCount: 1,
    isInviteOnly: false,
  },
  {
    id: 3,
    title: 'Pottery Workshop',
    hubId: 3,
    hubName: '32nd Avenue',
    category: 'ART',
    startTime: '2025-11-12T18:00:00+05:30',
    endTime: '2025-11-12T19:30:00+05:30',
    createdBy: 1,
    createdByName: 'Test User',
    peopleCount: 6,
    mutualsCount: 3,
    isInviteOnly: true,
  },
  {
    id: 4,
    title: 'Badminton Night',
    hubId: 1,
    hubName: 'Cyberhub',
    category: 'SPORTS',
    startTime: '2025-11-12T20:00:00+05:30',
    endTime: '2025-11-12T21:30:00+05:30',
    createdBy: 1,
    createdByName: 'Test User',
    peopleCount: 4,
    mutualsCount: 0,
    isInviteOnly: false,
  },
  {
    id: 5,
    title: 'Street Cricket',
    hubId: 1,
    hubName: 'Cyberhub',
    category: 'SPORTS',
    startTime: '2025-11-12T21:00:00+05:30',
    endTime: '2025-11-12T22:30:00+05:30',
    createdBy: 1,
    createdByName: 'Test User',
    peopleCount: 9,
    mutualsCount: 4,
    isInviteOnly: false,
  },
];

const MOCK_MESSAGES: Record<number, Message[]> = {
  1: [
    {
      id: 1,
      activityId: 1,
      userId: 1,
      userName: 'User #23',
      text: 'Hey! Looking forward to this!',
      createdAt: new Date().toISOString(),
    },
    {
      id: 2,
      activityId: 1,
      userId: 2,
      userName: 'User #45',
      text: 'Same here! See you there.',
      createdAt: new Date().toISOString(),
    },
  ],
};

// Token management
const TOKEN_KEY = 'gathr_token';

export const getToken = async (): Promise<string | null> => {
  try {
    return await SecureStore.getItemAsync(TOKEN_KEY);
  } catch (error) {
    console.error('Error getting token:', error);
    return null;
  }
};

export const setToken = async (token: string): Promise<void> => {
  try {
    await SecureStore.setItemAsync(TOKEN_KEY, token);
  } catch (error) {
    console.error('Error setting token:', error);
  }
};

export const clearToken = async (): Promise<void> => {
  try {
    await SecureStore.deleteItemAsync(TOKEN_KEY);
  } catch (error) {
    console.error('Error clearing token:', error);
  }
};

// API helper
const apiRequest = async <T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> => {
  const token = await getToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ error: 'Unknown error' }));
    throw new Error(error.error || `HTTP error! status: ${response.status}`);
  }

  return response.json();
};

// Auth API
export const authApi = {
  startOtp: async (phone: string): Promise<void> => {
    if (MOCK_MODE) {
      console.log('Mock: Sending OTP to', phone);
      return;
    }
    await apiRequest('/auth/otp/start', {
      method: 'POST',
      body: JSON.stringify({ phone }),
    });
  },

  verifyOtp: async (phone: string, otp: string): Promise<AuthResponse> => {
    if (MOCK_MODE) {
      const mockUser: User = {
        id: Math.floor(Math.random() * 1000),
        name: phone,
        phone,
        verified: true,
        createdAt: new Date().toISOString(),
      };
      const mockToken = 'mock_jwt_token_' + Date.now();
      await setToken(mockToken);
      return { token: mockToken, user: mockUser };
    }
    const response = await apiRequest<AuthResponse>('/auth/otp/verify', {
      method: 'POST',
      body: JSON.stringify({ phone, otp }),
    });
    await setToken(response.token);
    return response;
  },
};

// Hubs API
export const hubsApi = {
  getAll: async (): Promise<Hub[]> => {
    if (MOCK_MODE) {
      return MOCK_HUBS;
    }
    return apiRequest<Hub[]>('/hubs');
  },
};

// Activities API
export const activitiesApi = {
  getByHub: async (hubId: number, date?: string): Promise<Activity[]> => {
    if (MOCK_MODE) {
      const today = date || new Date().toISOString().split('T')[0];
      return MOCK_ACTIVITIES.filter(a => a.hubId === hubId);
    }
    const params = new URLSearchParams({ hub_id: hubId.toString() });
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
      const newActivity: Activity = {
        id: Math.floor(Math.random() * 10000),
        ...data,
        hubName: MOCK_HUBS.find(h => h.id === data.hubId)?.name || 'Unknown',
        createdBy: 1,
        createdByName: 'Current User',
        peopleCount: 1,
        mutualsCount: 0,
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

// Messages API
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

// Reports API
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

// Contacts API
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

// Events API (Telemetry)
export const eventsApi = {
  log: async (
    eventType: string,
    properties?: Record<string, any>,
    activityId?: number
  ): Promise<void> => {
    if (MOCK_MODE) {
      console.log('[Telemetry]', eventType, { activityId, ...properties });
      return;
    }

    try {
      // Non-blocking: fire and forget
      await apiRequest('/events', {
        method: 'POST',
        body: JSON.stringify({
          eventType,
          activityId,
          properties,
        }),
      });
    } catch (error) {
      // Silently fail - telemetry should never break the main flow
      console.warn('Failed to log event:', eventType, error);
    }
  },
};

// Templates API
export const templatesApi = {
  getAll: async (type: 'all' | 'system' | 'user' = 'all'): Promise<ActivityTemplate[]> => {
    if (MOCK_MODE) {
      const mockTemplates: ActivityTemplate[] = [
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
      return mockTemplates;
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

