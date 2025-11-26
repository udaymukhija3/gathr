import { useState, useCallback } from 'react';
import { Activity } from '../types';
import { activitiesApi, feedApi, FeedMeta } from '../services/api';
import { UserLocation } from '../services/location';
import { useFeedSession } from './useFeedSession';
import { trackFeed } from '../utils/telemetry';

export type FeedFilter = 'for_you' | 'all' | 'SPORTS' | 'FOOD' | 'ART' | 'MUSIC' | 'OUTDOOR' | 'GAMES' | 'LEARNING' | 'WELLNESS' | 'mutuals';

export const FILTER_OPTIONS: { key: FeedFilter; label: string; icon: string }[] = [
    { key: 'for_you', label: 'For You', icon: 'star' },
    { key: 'all', label: 'All', icon: 'apps' },
    { key: 'mutuals', label: 'Mutuals', icon: 'account-heart' },
    { key: 'SPORTS', label: 'Sports', icon: 'basketball' },
    { key: 'FOOD', label: 'Food', icon: 'food' },
    { key: 'ART', label: 'Art', icon: 'palette' },
    { key: 'MUSIC', label: 'Music', icon: 'music' },
    { key: 'OUTDOOR', label: 'Outdoors', icon: 'tree' },
    { key: 'GAMES', label: 'Games', icon: 'gamepad-variant' },
    { key: 'LEARNING', label: 'Learning', icon: 'book-open-variant' },
    { key: 'WELLNESS', label: 'Wellness', icon: 'meditation' },
];

interface UseFeedActivitiesProps {
    userInterests: string[];
}

export const useFeedActivities = ({ userInterests }: UseFeedActivitiesProps) => {
    const [activities, setActivities] = useState<Activity[]>([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [feedMeta, setFeedMeta] = useState<FeedMeta | null>(null);

    const { resetSession: resetFeedSession, markActivitiesViewed } = useFeedSession();

    // Sort activities to show user's interests first
    const sortByInterestRelevance = useCallback((activitiesList: Activity[], interests: string[]): Activity[] => {
        return [...activitiesList].sort((a, b) => {
            const aMatches = interests.includes(a.category);
            const bMatches = interests.includes(b.category);

            // Interest matches come first
            if (aMatches && !bMatches) return -1;
            if (!aMatches && bMatches) return 1;

            // Then by mutual count
            const aMutuals = a.mutualsCount || 0;
            const bMutuals = b.mutualsCount || 0;
            if (aMutuals !== bMutuals) return bMutuals - aMutuals;

            // Then by start time
            return new Date(a.startTime).getTime() - new Date(b.startTime).getTime();
        });
    }, []);

    const loadHubActivities = useCallback(async (
        hubId: number,
        filter: FeedFilter = 'all',
        options: { resetSession?: boolean } = {}
    ) => {
        try {
            if (options.resetSession !== false) {
                resetFeedSession();
            }
            setLoading(true);
            setError(null);
            const today = new Date().toISOString().split('T')[0];

            let activitiesData: Activity[];
            let payloadMeta: FeedMeta | null = null;

            switch (filter) {
                case 'for_you':
                    // Get personalized feed based on interests
                    const forYouPayload = await feedApi.getForYou(today);
                    activitiesData = forYouPayload.activities;
                    payloadMeta = forYouPayload.meta ?? null;
                    // Filter to selected hub if needed
                    if (hubId) {
                        activitiesData = activitiesData.filter(a => a.hubId === hubId);
                    }
                    break;
                case 'mutuals':
                    const mutualPayload = await feedApi.getWithMutuals(hubId, today);
                    activitiesData = mutualPayload.activities;
                    payloadMeta = mutualPayload.meta ?? null;
                    break;
                case 'SPORTS':
                case 'FOOD':
                case 'ART':
                case 'MUSIC':
                case 'OUTDOOR':
                case 'GAMES':
                case 'LEARNING':
                case 'WELLNESS':
                    const categoryPayload = await feedApi.getByCategory(hubId, filter, today);
                    activitiesData = categoryPayload.activities;
                    payloadMeta = categoryPayload.meta ?? null;
                    break;
                case 'all':
                default:
                    activitiesData = await activitiesApi.getByHub(hubId, today);
                    payloadMeta = null;
                    break;
            }

            // Sort activities to prioritize user's interests
            if (userInterests.length > 0 && filter === 'all') {
                activitiesData = sortByInterestRelevance(activitiesData, userInterests);
            }

            setActivities(activitiesData);
            setFeedMeta(payloadMeta);
            markActivitiesViewed(activitiesData);

            const hasPersonalization = activitiesData.some(item => (item.feedScore ?? 0) > 0);
            trackFeed.viewed(hubId, { activityCount: activitiesData.length, filter, hasPersonalization });
        } catch (err: any) {
            console.error('Error loading activities:', err);
            setError(err.message || 'Could not load activities');
        } finally {
            setLoading(false);
        }
    }, [userInterests, resetFeedSession, markActivitiesViewed, sortByInterestRelevance]);

    const loadNearbyActivities = useCallback(async (
        location: UserLocation,
        filter: FeedFilter = 'all',
        radius: number,
        options: { resetSession?: boolean } = {},
    ) => {
        try {
            if (options.resetSession !== false) {
                resetFeedSession();
            }
            setLoading(true);
            setError(null);
            let activitiesData = await activitiesApi.getNearby(location.latitude, location.longitude, radius);

            switch (filter) {
                case 'for_you':
                    activitiesData = sortByInterestRelevance(activitiesData, userInterests);
                    break;
                case 'mutuals':
                    activitiesData = activitiesData.filter((activity) => (activity.mutualsCount || 0) > 0);
                    break;
                case 'all':
                    break;
                default:
                    activitiesData = activitiesData.filter((activity) => activity.category === filter);
            }

            setActivities(activitiesData);
            setFeedMeta(null);
            markActivitiesViewed(activitiesData);
            trackFeed.viewed(undefined, { mode: 'nearby', radiusKm: radius });
        } catch (err: any) {
            console.error('Error loading nearby activities:', err);
            setError(err.message || 'Could not load nearby activities');
        } finally {
            setLoading(false);
        }
    }, [userInterests, resetFeedSession, markActivitiesViewed, sortByInterestRelevance]);

    const refresh = useCallback(async (
        mode: 'hub' | 'nearby',
        hubId: number | null,
        location: UserLocation | null,
        filter: FeedFilter,
        radius: number
    ) => {
        setRefreshing(true);
        if (mode === 'nearby' && location) {
            trackFeed.refreshed(undefined, { mode: 'nearby' });
            await loadNearbyActivities(location, filter, radius);
        } else if (hubId) {
            trackFeed.refreshed(hubId);
            await loadHubActivities(hubId, filter);
        }
        setRefreshing(false);
    }, [loadHubActivities, loadNearbyActivities]);

    return {
        activities,
        loading,
        refreshing,
        error,
        feedMeta,
        loadHubActivities,
        loadNearbyActivities,
        refresh
    };
};
