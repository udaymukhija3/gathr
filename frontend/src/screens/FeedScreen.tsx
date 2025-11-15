import React, { useEffect, useState } from 'react';
import { View, StyleSheet, FlatList, RefreshControl, TouchableOpacity } from 'react-native';
import { Text, FAB, ActivityIndicator } from 'react-native-paper';
import { Activity, Hub } from '../types';
import { ActivityCard } from '../components/ActivityCard';
import { HubSelector } from '../components/HubSelector';
import { activitiesApi, hubsApi } from '../services/api';

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
    } catch (error) {
      console.error('Error loading activities:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    if (selectedHubId) {
      await loadActivities(selectedHubId);
    }
    setRefreshing(false);
  };

  const handleJoin = async (activityId: number) => {
    try {
      await activitiesApi.join(activityId, 'INTERESTED');
      // Refresh activities
      if (selectedHubId) {
        await loadActivities(selectedHubId);
      }
    } catch (error) {
      console.error('Error joining activity:', error);
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
        onSelectHub={setSelectedHubId}
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
        onPress={onCreateActivity}
        label="Create your plan"
      />
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

