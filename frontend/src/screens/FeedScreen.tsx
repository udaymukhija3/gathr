import React, { useCallback, useEffect, useRef, useState } from 'react';
import { View, StyleSheet, FlatList, RefreshControl, ScrollView } from 'react-native';
import { Text, FAB, IconButton, Button, Card, Chip, Badge } from 'react-native-paper';
import Toast from 'react-native-toast-message';
import * as SecureStore from 'expo-secure-store';
import { Activity, Hub } from '../types';
import { ActivityCard } from '../components/ActivityCard';
import { HubSelector } from '../components/HubSelector';
import { InviteModal } from '../components/InviteModal';
import { FeedSkeleton } from '../components/SkeletonLoader';
import { activitiesApi, hubsApi, feedApi, FeedMeta } from '../services/api';
import { UserLocation, getCurrentLocation } from '../services/location';
import { trackFeed, trackActivity } from '../utils/telemetry';
import { useUser } from '../context/UserContext';
import { useFeedSession } from '../hooks/useFeedSession';

// Feed filter options - extended categories
type FeedFilter = 'for_you' | 'all' | 'SPORTS' | 'FOOD' | 'ART' | 'MUSIC' | 'OUTDOOR' | 'GAMES' | 'LEARNING' | 'WELLNESS' | 'mutuals';

const FILTER_OPTIONS: { key: FeedFilter; label: string; icon: string }[] = [

const HUB_STORAGE_KEY = 'feed:selectedHubId';
type FeedFeedbackAction = 'viewed' | 'clicked' | 'joined' | 'dismissed';
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

interface FeedScreenProps {
  onCreateActivity: () => void;
  onActivityPress: (activityId: number) => void;
  navigation?: any;
}

export const FeedScreen: React.FC<FeedScreenProps> = ({
  onCreateActivity,
  onActivityPress,
  navigation,
}) => {
  const { user } = useUser();
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [selectedHubId, setSelectedHubId] = useState<number | null>(null);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [joiningActivityId, setJoiningActivityId] = useState<number | null>(null);
  const [joinedActivityIds, setJoinedActivityIds] = useState<Set<number>>(new Set());
  const [activeFilter, setActiveFilter] = useState<FeedFilter>('for_you');
  const [feedMode, setFeedMode] = useState<'hub' | 'nearby'>('hub');
  const [userLocation, setUserLocation] = useState<UserLocation | null>(null);
  const [requestingLocation, setRequestingLocation] = useState(false);
  const [radiusKm, setRadiusKm] = useState(5);
  const [feedMeta, setFeedMeta] = useState<FeedMeta | null>(null);
  const { resetSession: resetFeedSession, markActivitiesViewed, sendFeedback } = useFeedSession();

  // Determine which filters to highlight based on user interests
  const userInterests = user?.interests || [];

  useEffect(() => {
    loadHubs();
  }, []);

  useEffect(() => {
    if (user?.homeHubId && !selectedHubId) {
      setSelectedHubId(user.homeHubId);
      void SecureStore.setItemAsync(HUB_STORAGE_KEY, user.homeHubId.toString());
    }
  }, [user, selectedHubId]);

  useEffect(() => {
    if (feedMode === 'hub') {
      if (selectedHubId) {
        loadHubActivities(selectedHubId, activeFilter);
      } else if (hubs.length > 0) {
        setSelectedHubId(hubs[0].id);
      }
    } else if (feedMode === 'nearby' && userLocation) {
      loadNearbyActivities(userLocation, activeFilter, radiusKm);
    }
  }, [feedMode, selectedHubId, hubs, activeFilter, userLocation, radiusKm]);

  const persistSelectedHub = useCallback(async (hubId: number) => {
    try {
      await SecureStore.setItemAsync(HUB_STORAGE_KEY, hubId.toString());
    } catch (err) {
      console.warn('Failed to persist hub selection', err);
    }
  }, []);

  const loadHubs = async () => {
    try {
      const hubsData = await hubsApi.getAll();
      setHubs(hubsData);

      const storedHubId = await SecureStore.getItemAsync(HUB_STORAGE_KEY);
      if (storedHubId) {
        const parsed = Number(storedHubId);
        if (!Number.isNaN(parsed)) {
          setSelectedHubId(parsed);
          return;
        }
      }

      if (user?.homeHubId) {
        setSelectedHubId(user.homeHubId);
        await persistSelectedHub(user.homeHubId);
        return;
      }

      if (hubsData.length > 0 && !selectedHubId) {
        setSelectedHubId(hubsData[0].id);
      }
    } catch (error) {
      console.error('Error loading hubs:', error);
    }
  };

  const loadHubActivities = async (
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
  };

  const loadNearbyActivities = async (
    location: UserLocation,
    filter: FeedFilter = 'all',
    radius: number = radiusKm,
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
  };

  const handleCardPress = useCallback(
    (activity: Activity, position: number) => {
      sendFeedback('clicked', activity, position);
      onActivityPress(activity.id);
    },
    [onActivityPress, sendFeedback]
  );

  // Sort activities to show user's interests first
  const sortByInterestRelevance = (activities: Activity[], interests: string[]): Activity[] => {
    return [...activities].sort((a, b) => {
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
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    if (feedMode === 'nearby' && userLocation) {
      trackFeed.refreshed(undefined, { mode: 'nearby' });
      await loadNearbyActivities(userLocation, activeFilter, radiusKm);
    } else if (selectedHubId) {
      trackFeed.refreshed(selectedHubId);
      await loadHubActivities(selectedHubId, activeFilter);
    }
    setRefreshing(false);
  };

  const handleFilterChange = (filter: FeedFilter) => {
    if (filter !== activeFilter) {
      setActiveFilter(filter);
      resetFeedSession();
      trackFeed.filterChanged?.(selectedHubId || 0, filter);
    }
  };

  // Render filter chips
  const renderFilterChips = () => (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      style={styles.filterContainer}
      contentContainerStyle={styles.filterContent}
    >
      {FILTER_OPTIONS.map((option) => {
        const isActive = activeFilter === option.key;
        const isUserInterest = userInterests.includes(option.key);

        return (
          <Chip
            key={option.key}
            icon={option.icon}
            selected={isActive}
            onPress={() => handleFilterChange(option.key)}
            style={[
              styles.filterChip,
              isActive && styles.filterChipActive,
              isUserInterest && !isActive && styles.filterChipInterest,
            ]}
            textStyle={[
              styles.filterChipText,
              isActive && styles.filterChipTextActive,
            ]}
          >
            {option.label}
            {isUserInterest && !isActive && (
              <Text style={styles.interestDot}> •</Text>
            )}
          </Chip>
        );
      })}
    </ScrollView>
  );

  const enableNearbyMode = async () => {
    if (!userLocation) {
      setRequestingLocation(true);
      const location = await getCurrentLocation();
      setRequestingLocation(false);
      if (!location) {
        Toast.show({
          type: 'error',
          text1: 'Location needed',
          text2: 'Enable location services to see nearby plans',
        });
        return;
      }
      setUserLocation(location);
    }
    setFeedMeta(null);
    resetFeedSession();
    setFeedMode('nearby');
  };

  const disableNearbyMode = () => {
    resetFeedSession();
    setFeedMode('hub');
  };

  const handleJoin = async (activityId: number, position?: number, inviteToken?: string) => {
    const activity = activities.find(a => a.id === activityId);

    // Check if invite-only and no token provided
    if (activity?.isInviteOnly && !inviteToken) {
      trackActivity.inviteModalOpened(activityId, { source: 'feed' });
      setJoiningActivityId(activityId);
      setShowInviteModal(true);
      return;
    }

    // Check max members
    if (activity?.maxMembers && (activity.peopleCount || 0) >= activity.maxMembers) {
      trackActivity.full(activityId, {
        maxMembers: activity.maxMembers,
        currentCount: activity.peopleCount,
      });
      Toast.show({
        type: 'error',
        text1: 'Activity Full',
        text2: `This activity has reached maximum participants (${activity.maxMembers})`,
      });
      return;
    }

    try {
      await activitiesApi.join(activityId, 'INTERESTED', inviteToken);
      trackActivity.joined(activityId, 'INTERESTED', {
        source: 'feed',
        usedInviteToken: !!inviteToken,
      });
      if (activity) {
        sendFeedback('joined', activity, position);
      }

      // Track locally for immediate UI feedback
      setJoinedActivityIds((prev) => new Set([...prev, activityId]));

      Toast.show({
        type: 'success',
        text1: 'Joined!',
        text2: 'You\'re interested in this activity',
      });
      // Refresh activities
      if (selectedHubId) {
        await loadHubActivities(selectedHubId, activeFilter, { resetSession: false });
      }
      setShowInviteModal(false);
      setJoiningActivityId(null);
    } catch (error: any) {
      const errorMessage = error.message || 'Failed to join activity';
      if (errorMessage.includes('403') || errorMessage.includes('Forbidden')) {
        trackActivity.inviteModalOpened(activityId, { source: 'feed', reason: 'forbidden' });
        Toast.show({
          type: 'error',
          text1: 'Invite Required',
          text2: 'This is an invite-only activity. Please enter a valid invite token.',
        });
        setJoiningActivityId(activityId);
        setShowInviteModal(true);
      } else if (errorMessage.includes('409') || errorMessage.includes('Conflict')) {
        trackActivity.full(activityId, { error: 'conflict' });
        Toast.show({
          type: 'error',
          text1: 'Activity Full',
          text2: 'This activity has reached maximum participants',
        });
      } else {
        Toast.show({
          type: 'error',
          text1: 'Error',
          text2: errorMessage,
        });
      }
    }
  };

  // Show skeleton while initial loading
  if (loading && activities.length === 0 && !error) {
    return (
      <View style={styles.container}>
        <View style={styles.header}>
          <Text variant="headlineSmall" style={styles.title}>
            Tonight
          </Text>
          <View style={styles.headerActions}>
            <IconButton icon="bell-outline" size={24} disabled />
            <IconButton icon="calendar-check" size={24} disabled />
            <IconButton icon="account-circle" size={24} disabled />
          </View>
        </View>
        <FeedSkeleton count={4} />
      </View>
    );
  }

  // Error state with retry
  const renderErrorState = () => (
    <Card style={styles.errorCard}>
      <Card.Content style={styles.errorContent}>
        <IconButton icon="alert-circle-outline" size={48} iconColor="#B00020" />
        <Text variant="titleMedium" style={styles.errorTitle}>
          Couldn't load activities
        </Text>
        <Text variant="bodyMedium" style={styles.errorMessage}>
          {error}
        </Text>
        <Button
          mode="contained"
          onPress={() => {
            if (feedMode === 'nearby' && userLocation) {
              loadNearbyActivities(userLocation, activeFilter, radiusKm);
            } else if (selectedHubId) {
              loadHubActivities(selectedHubId);
            }
          }}
          style={styles.retryButton}
        >
          Try Again
        </Button>
      </Card.Content>
    </Card>
  );

  // Empty state
  const renderEmptyState = () => (
    <View style={styles.empty}>
      <IconButton icon="calendar-blank-outline" size={64} iconColor="#CCC" />
      <Text variant="titleMedium" style={styles.emptyTitle}>
        No activities tonight
      </Text>
      <Text variant="bodyMedium" style={styles.emptyText}>
        Be the first to create one in this hub!
      </Text>
      <Button
        mode="contained"
        onPress={() => {
          trackFeed.createActivityClicked({ hubId: selectedHubId, source: 'empty_state' });
          onCreateActivity();
        }}
        style={styles.createButton}
        icon="plus"
      >
        Create Activity
      </Button>
    </View>
  );

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text variant="headlineSmall" style={styles.title}>
          Tonight
        </Text>
        <View style={styles.headerActions}>
          <IconButton
            icon="bell-outline"
            size={24}
            onPress={() => navigation?.navigate('Notifications')}
          />
          <IconButton
            icon="calendar-check"
            size={24}
            onPress={() => navigation?.navigate('MyActivities')}
          />
          <IconButton
            icon="account-circle"
            size={24}
            onPress={() => navigation?.navigate('Profile')}
          />
        </View>
      </View>

      {feedMode === 'hub' && feedMeta && (
        <View style={styles.metaBanner}>
          {feedMeta.ctaText && (
            <Text variant="titleMedium" style={styles.metaCta}>
              {feedMeta.ctaText}
            </Text>
          )}
          {feedMeta.scarcityMessage && (
            <Text variant="bodySmall" style={styles.metaSub}>
              {feedMeta.scarcityMessage}
            </Text>
          )}
          {feedMeta.timeWindowLabel && (
            <Text variant="bodySmall" style={styles.metaWindow}>
              {feedMeta.timeWindowLabel}
            </Text>
          )}
        </View>
      )}

      <View style={styles.modeToggle}>
        <Chip
          icon="domain"
          selected={feedMode === 'hub'}
          onPress={disableNearbyMode}
          style={styles.modeChip}
        >
          Curated Hubs
        </Chip>
        <Chip
          icon="map-marker-radius"
          selected={feedMode === 'nearby'}
          onPress={enableNearbyMode}
          style={styles.modeChip}
          disabled={requestingLocation}
        >
          {requestingLocation ? 'Locating...' : 'Near Me'}
        </Chip>
      </View>

      {feedMode === 'hub' ? (
        <HubSelector
          hubs={hubs}
          selectedHubId={selectedHubId}
          onSelectHub={(hubId) => {
            const hub = hubs.find((h) => h.id === hubId);
            if (hub) {
              trackFeed.hubSelected(hubId, hub.name);
            }
            setSelectedHubId(hubId);
            void persistSelectedHub(hubId);
            resetFeedSession();
          }}
        />
      ) : (
        <View style={styles.radiusContainer}>
          <Text variant="bodySmall" style={styles.helperText}>
            Choose a radius
          </Text>
          <View style={styles.radiusChips}>
            {[5, 10, 25].map((km) => (
              <Chip
                key={km}
                selected={radiusKm === km}
                onPress={() => setRadiusKm(km)}
                style={styles.radiusChip}
              >
                {km} km
              </Chip>
            ))}
          </View>
          {userLocation ? (
            <Text variant="bodySmall" style={styles.helperText}>
              Showing plans within {radiusKm} km of you
            </Text>
          ) : (
            <Text variant="bodySmall" style={styles.helperText}>
              Enable location to view nearby plans
            </Text>
          )}
        </View>
      )}

      {/* Smart Filter Chips */}
      {renderFilterChips()}

      {error ? (
        renderErrorState()
      ) : (
        <FlatList
          data={activities}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item, index }) => (
            <ActivityCard
              activity={item}
              onPress={() => handleCardPress(item, index)}
              onJoin={() => handleJoin(item.id, index)}
              isJoined={joinedActivityIds.has(item.id)}
            />
          )}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
          }
          ListEmptyComponent={renderEmptyState}
        />
      )}

      <FAB
        icon="map-marker"
        style={styles.fabMap}
        onPress={() => navigation?.navigate('MapView')}
        small
      />

      <FAB
        icon="plus"
        style={styles.fab}
        onPress={() => {
          trackFeed.createActivityClicked({ hubId: selectedHubId });
          onCreateActivity();
        }}
        label="Create your plan"
      />

      {joiningActivityId && (
        <InviteModal
          visible={showInviteModal}
          activityId={joiningActivityId}
          onClose={() => {
            setShowInviteModal(false);
            setJoiningActivityId(null);
          }}
          onTokenEntered={(token) => handleJoin(joiningActivityId, undefined, token)}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  header: {
    padding: 16,
    paddingRight: 8,
    backgroundColor: '#FFF',
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  headerActions: {
    flexDirection: 'row',
  },
  title: {
    fontWeight: 'bold',
  },
  metaBanner: {
    backgroundColor: '#F0F4FF',
    marginHorizontal: 16,
    marginTop: 8,
    marginBottom: 4,
    borderRadius: 12,
    padding: 12,
  },
  metaCta: {
    fontWeight: '600',
    color: '#1A237E',
  },
  metaSub: {
    color: '#3949AB',
    marginTop: 4,
  },
  metaWindow: {
    color: '#5C6BC0',
    marginTop: 2,
    fontStyle: 'italic',
  },
  modeToggle: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    marginTop: 8,
    gap: 8,
  },
  modeChip: {
    flex: 1,
  },
  radiusContainer: {
    paddingHorizontal: 16,
    paddingBottom: 8,
  },
  radiusChips: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 8,
  },
  radiusChip: {},
  listContent: {
    paddingBottom: 80,
  },
  empty: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
    paddingBottom: 100,
  },
  emptyTitle: {
    color: '#333',
    marginTop: 8,
    textAlign: 'center',
  },
  emptyText: {
    color: '#666',
    textAlign: 'center',
    marginTop: 4,
  },
  createButton: {
    marginTop: 24,
  },
  errorCard: {
    margin: 16,
    marginTop: 32,
  },
  errorContent: {
    alignItems: 'center',
    padding: 16,
  },
  errorTitle: {
    color: '#333',
    marginTop: 8,
  },
  errorMessage: {
    color: '#666',
    textAlign: 'center',
    marginTop: 4,
  },
  retryButton: {
    marginTop: 16,
  },
  fab: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 0,
  },
  fabMap: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 70,
    backgroundColor: '#FFF',
  },
  filterContainer: {
    backgroundColor: '#FFF',
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
    maxHeight: 56,
  },
  filterContent: {
    paddingHorizontal: 12,
    paddingVertical: 10,
    gap: 8,
  },
  filterChip: {
    backgroundColor: '#F5F5F5',
    marginRight: 4,
  },
  filterChipActive: {
    backgroundColor: '#6200EE',
  },
  filterChipInterest: {
    backgroundColor: '#F3E5F5',
    borderColor: '#6200EE',
    borderWidth: 1,
  },
  filterChipText: {
    fontSize: 13,
    color: '#333',
  },
  filterChipTextActive: {
    color: '#FFF',
  },
  interestDot: {
    color: '#6200EE',
    fontWeight: 'bold',
  },
});

