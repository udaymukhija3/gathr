import React, { useEffect, useState } from 'react';
import { View, StyleSheet, FlatList, RefreshControl } from 'react-native';
import { Text, FAB, ActivityIndicator } from 'react-native-paper';
import Toast from 'react-native-toast-message';
import { Activity, Hub } from '../types';
import { ActivityCard } from '../components/ActivityCard';
import { HubSelector } from '../components/HubSelector';
import { InviteModal } from '../components/InviteModal';
import { activitiesApi, hubsApi } from '../services/api';
import { trackFeed, trackActivity } from '../utils/telemetry';

interface FeedScreenProps {
  onCreateActivity: () => void;
  onActivityPress: (activityId: number) => void;
}

export const FeedScreen: React.FC<FeedScreenProps> = ({
  onCreateActivity,
  onActivityPress,
}) => {
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [selectedHubId, setSelectedHubId] = useState<number | null>(null);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [joiningActivityId, setJoiningActivityId] = useState<number | null>(null);

  useEffect(() => {
    loadHubs();
  }, []);

  useEffect(() => {
    if (selectedHubId) {
      loadActivities(selectedHubId);
    } else if (hubs.length > 0) {
      // Select first hub by default
      setSelectedHubId(hubs[0].id);
    }
  }, [selectedHubId, hubs]);

  const loadHubs = async () => {
    try {
      const hubsData = await hubsApi.getAll();
      setHubs(hubsData);
      if (hubsData.length > 0 && !selectedHubId) {
        setSelectedHubId(hubsData[0].id);
      }
    } catch (error) {
      console.error('Error loading hubs:', error);
    }
  };

  const loadActivities = async (hubId: number) => {
    try {
      setLoading(true);
      const today = new Date().toISOString().split('T')[0];
      const activitiesData = await activitiesApi.getByHub(hubId, today);
      setActivities(activitiesData);

      // Track feed viewed
      trackFeed.viewed(hubId, { activityCount: activitiesData.length });
    } catch (error) {
      console.error('Error loading activities:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    if (selectedHubId) {
      trackFeed.refreshed(selectedHubId);
      await loadActivities(selectedHubId);
    }
    setRefreshing(false);
  };

  const handleJoin = async (activityId: number, inviteToken?: string) => {
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
      Toast.show({
        type: 'success',
        text1: 'Joined!',
        text2: 'You\'re interested in this activity',
      });
      // Refresh activities
      if (selectedHubId) {
        await loadActivities(selectedHubId);
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

  if (loading && activities.length === 0) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text variant="headlineSmall" style={styles.title}>
          Tonight in Gurgaon
        </Text>
      </View>

      <HubSelector
        hubs={hubs}
        selectedHubId={selectedHubId}
        onSelectHub={(hubId) => {
          const hub = hubs.find((h) => h.id === hubId);
          if (hub) {
            trackFeed.hubSelected(hubId, hub.name);
          }
          setSelectedHubId(hubId);
        }}
      />

      <FlatList
        data={activities}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => (
          <ActivityCard
            activity={item}
            onPress={() => onActivityPress(item.id)}
            onJoin={() => handleJoin(item.id)}
          />
        )}
        contentContainerStyle={styles.listContent}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
        }
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text variant="bodyLarge" style={styles.emptyText}>
              No activities tonight in this hub
            </Text>
          </View>
        }
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
          onTokenEntered={(token) => handleJoin(joiningActivityId, token)}
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
    backgroundColor: '#FFF',
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  title: {
    fontWeight: 'bold',
  },
  listContent: {
    paddingBottom: 80,
  },
  empty: {
    padding: 32,
    alignItems: 'center',
  },
  emptyText: {
    color: '#666',
  },
  fab: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 0,
  },
});

