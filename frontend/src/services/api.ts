import * as SecureStore from 'expo-secure-store';
import { Activity, ActivityDetail, AuthResponse, CreateActivityRequest, Hub, Message, User } from '../types';

// Mock mode: Set to true to use mock data, false to use real API
// Can be toggled via EXPO_PUBLIC_MOCK_MODE environment variable
const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080';
const MOCK_MODE = process.env.EXPO_PUBLIC_MOCK_MODE !== 'false'; // Default to true (mock mode)

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

  join: async (id: number, status: 'INTERESTED' | 'CONFIRMED'): Promise<void> => {
    if (MOCK_MODE) {
      console.log('Mock: Joining activity', id, 'with status', status);
      const activity = MOCK_ACTIVITIES.find(a => a.id === id);
      if (activity) {
        activity.peopleCount = (activity.peopleCount || 0) + 1;
      }
      return;
    }
    await apiRequest(`/activities/${id}/join?status=${status}`, {
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
  getByActivity: async (activityId: number): Promise<Message[]> => {
    if (MOCK_MODE) {
      return MOCK_MESSAGES[activityId] || [];
    }
    return apiRequest<Message[]>(`/activities/${activityId}/messages`);
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

