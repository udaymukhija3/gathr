import React, { useCallback, useEffect, useState } from 'react';
import { View, StyleSheet, FlatList, RefreshControl } from 'react-native';
import { Text, FAB, IconButton, Button, Card, Chip } from 'react-native-paper';
import Toast from 'react-native-toast-message';
import * as SecureStore from 'expo-secure-store';
import { Activity, Hub } from '../types';
import { ActivityCard } from '../components/ActivityCard';
import { HubSelector } from '../components/HubSelector';
import { InviteModal } from '../components/InviteModal';
import { FeedSkeleton } from '../components/SkeletonLoader';
import { hubsApi, activitiesApi } from '../services/api';
import { trackFeed, trackActivity } from '../utils/telemetry';
import { useUser } from '../context/UserContext';
import { useFeedSession } from '../hooks/useFeedSession';
import { useFeedActivities, FeedFilter } from '../hooks/useFeedActivities';
import { useFeedLocation } from '../hooks/useFeedLocation';
import { FeedHeader } from '../components/feed/FeedHeader';
import { FeedFilters } from '../components/feed/FeedFilters';
import { FeedModeToggle } from '../components/feed/FeedModeToggle';

const HUB_STORAGE_KEY = 'feed:selectedHubId';

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
  const [activeFilter, setActiveFilter] = useState<FeedFilter>('for_you');
  const [feedMode, setFeedMode] = useState<'hub' | 'nearby'>('hub');
  const [radiusKm, setRadiusKm] = useState(5);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [joiningActivityId, setJoiningActivityId] = useState<number | null>(null);
  const [joinedActivityIds, setJoinedActivityIds] = useState<Set<number>>(new Set());

  const { sendFeedback, resetSession } = useFeedSession();

  const {
    activities,
    loading,
    refreshing,
    error,
    feedMeta,
    loadHubActivities,
    loadNearbyActivities,
    refresh
  } = useFeedActivities({ userInterests: user?.interests || [] });

  const {
    userLocation,
    requestingLocation,
    requestLocation,
    setUserLocation
  } = useFeedLocation();

  // Load Hubs & Initial Selection
  useEffect(() => {
    loadHubs();
  }, []);

  useEffect(() => {
    if (user?.homeHubId && !selectedHubId) {
      setSelectedHubId(user.homeHubId);
      void SecureStore.setItemAsync(HUB_STORAGE_KEY, user.homeHubId.toString());
    }
  }, [user, selectedHubId]);

  // Trigger Feed Load
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
  }, [feedMode, selectedHubId, hubs, activeFilter, userLocation, radiusKm, loadHubActivities, loadNearbyActivities]);

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

  const handleRefresh = async () => {
    await refresh(feedMode, selectedHubId, userLocation, activeFilter, radiusKm);
  };

  const handleFilterChange = (filter: FeedFilter) => {
    if (filter !== activeFilter) {
      setActiveFilter(filter);
      resetSession();
      trackFeed.filterChanged?.(selectedHubId || 0, filter);
    }
  };

  const enableNearbyMode = async () => {
    if (!userLocation) {
      const location = await requestLocation();
      if (!location) return;
    }
    resetSession();
    setFeedMode('nearby');
  };

  const disableNearbyMode = () => {
    resetSession();
    setFeedMode('hub');
  };

  const handleCardPress = useCallback(
    (activity: Activity, position: number) => {
      sendFeedback('clicked', activity, position);
      onActivityPress(activity.id);
    },
    [onActivityPress, sendFeedback]
  );

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

      // Refresh activities without resetting session
      if (feedMode === 'hub' && selectedHubId) {
        loadHubActivities(selectedHubId, activeFilter, { resetSession: false });
      } else if (feedMode === 'nearby' && userLocation) {
        loadNearbyActivities(userLocation, activeFilter, radiusKm, { resetSession: false });
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

  // Render Methods
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
          onPress={handleRefresh}
          style={styles.retryButton}
        >
          Try Again
        </Button>
      </Card.Content>
    </Card>
  );

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

  // Loading Skeleton
  if (loading && activities.length === 0 && !error) {
    return (
      <View style={styles.container}>
        <FeedHeader
          onNotificationPress={() => navigation?.navigate('Notifications')}
          onMyActivitiesPress={() => navigation?.navigate('MyActivities')}
          onProfilePress={() => navigation?.navigate('Profile')}
        />
        <FeedSkeleton count={4} />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <FeedHeader
        onNotificationPress={() => navigation?.navigate('Notifications')}
        onMyActivitiesPress={() => navigation?.navigate('MyActivities')}
        onProfilePress={() => navigation?.navigate('Profile')}
      />

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

      <FeedModeToggle
        feedMode={feedMode}
        requestingLocation={requestingLocation}
        onEnableNearby={enableNearbyMode}
        onDisableNearby={disableNearbyMode}
      />

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
            resetSession();
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

      <FeedFilters
        activeFilter={activeFilter}
        onFilterChange={handleFilterChange}
        userInterests={user?.interests || []}
      />

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
  helperText: {
    color: '#666',
    marginBottom: 4,
  },
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
});
