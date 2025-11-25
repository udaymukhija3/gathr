import { Activity, FeedMetaPayload } from '../../types';
import { apiRequest, MOCK_MODE } from './client';
import { MOCK_ACTIVITIES } from './mockData';

type BackendFeedItem = {
  activity: Activity;
  score: number;
  primaryReason?: string;
  secondaryReason?: string;
  mutualCount?: number | null;
  spotsRemaining?: number | null;
  metadata?: Record<string, any>;
};

type BackendFeedResponse = {
  activities: BackendFeedItem[];
  hubId?: number;
  date?: string;
  totalCount: number;
  userId: number;
  fallbackUsed?: boolean;
  suggestions?: string[];
  meta?: FeedMetaPayload;
};

type FeedFeedbackAction = 'viewed' | 'clicked' | 'joined' | 'dismissed';
type FeedFeedbackPayload = {
  activityId: number;
  action: FeedFeedbackAction;
  position?: number;
  score?: number;
  hubId?: number;
  sessionId?: string;
};

type FeedStats = {
  userId: number;
  personalizationEnabled: boolean;
  interestsCount: number;
  mutualConnectionsCount: number;
  homeHubId?: number | null;
  preferredRadiusKm?: number | null;
  lastActive?: string | null;
};

export interface FeedMeta extends FeedMetaPayload {}

export interface FeedPayload {
  activities: Activity[];
  meta?: FeedMeta;
  fallbackUsed?: boolean;
  suggestions?: string[];
}

const mapFeedItems = (items: BackendFeedItem[]): Activity[] =>
  items.map(item => ({
    ...item.activity,
    feedScore: item.score,
    feedPrimaryReason: item.primaryReason,
    feedSecondaryReason: item.secondaryReason,
    mutualsCount: item.mutualCount ?? item.activity.mutualsCount,
    spotsRemaining: item.spotsRemaining ?? item.activity.spotsRemaining,
    recommendationMeta: item.metadata,
    coldStartType: item.metadata?.coldStartType,
    isNewActivity: Boolean(item.metadata?.newActivityBoost),
  }));

const coerceFeedResponse = (payload: BackendFeedResponse | BackendFeedItem[]): BackendFeedResponse => {
  if (Array.isArray(payload)) {
    return {
      activities: payload,
      totalCount: payload.length,
      userId: 0,
      date: new Date().toISOString().split('T')[0],
      hubId: undefined,
      fallbackUsed: false,
      suggestions: [],
      meta: undefined,
    };
  }
  return payload;
};

const requestFeed = async (params: Record<string, string | number | undefined>): Promise<FeedPayload> => {
  if (MOCK_MODE) {
    return { activities: MOCK_ACTIVITIES };
  }

  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null) return;
    search.append(key, String(value));
  });
  const query = search.toString();
  const endpoint = query ? `/feed?${query}` : '/feed';
  const payload = await apiRequest<BackendFeedResponse | BackendFeedItem[]>(endpoint);
  const coerced = coerceFeedResponse(payload);
  return {
    activities: mapFeedItems(coerced.activities),
    meta: coerced.meta,
    fallbackUsed: coerced.fallbackUsed,
    suggestions: coerced.suggestions,
  };
};

export const feedApi = {
  getPersonalized: async (hubId: number, date?: string): Promise<FeedPayload> => {
    if (MOCK_MODE) {
      return { activities: MOCK_ACTIVITIES.filter(a => a.hubId === hubId) };
    }
    return requestFeed({ hubId, limit: 50, date });
  },

  getByCategory: async (hubId: number, category: string, date?: string): Promise<FeedPayload> => {
    if (MOCK_MODE) {
      return { activities: MOCK_ACTIVITIES.filter(a => a.hubId === hubId && a.category === category) };
    }
    const payload = await requestFeed({ hubId, limit: 50, date });
    return {
      ...payload,
      activities: payload.activities.filter(a => a.category === category),
    };
  },

  getForYou: async (date?: string): Promise<FeedPayload> => {
    if (MOCK_MODE) {
      return { activities: MOCK_ACTIVITIES.filter(a => ['SPORTS', 'FOOD'].includes(a.category)) };
    }
    return requestFeed({ limit: 50, date });
  },

  getWithMutuals: async (hubId: number, date?: string): Promise<FeedPayload> => {
    if (MOCK_MODE) {
      return { activities: MOCK_ACTIVITIES.filter(a => a.hubId === hubId && (a.mutualsCount || 0) > 0) };
    }
    const payload = await requestFeed({ hubId, limit: 50, date });
    return {
      ...payload,
      activities: payload.activities.filter(a => (a.mutualsCount || 0) > 0),
    };
  },

  trackFeedback: async (payload: FeedFeedbackPayload): Promise<void> => {
    if (MOCK_MODE) {
      console.log('[Mock feed feedback]', payload);
      return;
    }
    await apiRequest('/feed/feedback', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  refreshCache: async (): Promise<void> => {
    if (MOCK_MODE) {
      return;
    }
    await apiRequest('/feed/refresh', {
      method: 'POST',
    });
  },

  getStats: async (): Promise<FeedStats> => {
    if (MOCK_MODE) {
      return {
        userId: 1,
        personalizationEnabled: true,
        interestsCount: 2,
        mutualConnectionsCount: 4,
        homeHubId: 1,
        preferredRadiusKm: 5,
        lastActive: new Date().toISOString(),
      };
    }
    return apiRequest<FeedStats>('/feed/stats');
  },
};

