import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, FlatList, RefreshControl } from 'react-native';
import {
  Text,
  Card,
  Chip,
  ActivityIndicator,
  SegmentedButtons,
  Badge,
  Button,
  IconButton,
} from 'react-native-paper';
import Toast from 'react-native-toast-message';
import { usersApi, feedbackApi, FeedbackRequest } from '../services/api';
import { UserActivity, ActivityCategory, Activity } from '../types';
import { FeedbackModal } from '../components/FeedbackModal';

const CATEGORY_COLORS: Record<ActivityCategory, string> = {
  SPORTS: '#4CAF50',
  FOOD: '#FF9800',
  ART: '#9C27B0',
  MUSIC: '#2196F3',
  OUTDOOR: '#00BCD4',
  GAMES: '#FF5722',
  LEARNING: '#3F51B5',
  WELLNESS: '#E91E63',
};

const CATEGORY_ICONS: Record<ActivityCategory, string> = {
  SPORTS: 'basketball',
  FOOD: 'food',
  ART: 'palette',
  MUSIC: 'music',
  OUTDOOR: 'tree',
  GAMES: 'gamepad-variant',
  LEARNING: 'book-open-page-variant',
  WELLNESS: 'meditation',
};

interface MyActivitiesScreenProps {
  navigation?: any;
  onActivityPress?: (activityId: number) => void;
}

export const MyActivitiesScreen: React.FC<MyActivitiesScreenProps> = ({
  navigation,
  onActivityPress,
}) => {
  const [activities, setActivities] = useState<UserActivity[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [filter, setFilter] = useState<'upcoming' | 'past'>('upcoming');
  const [feedbackModalVisible, setFeedbackModalVisible] = useState(false);
  const [selectedActivity, setSelectedActivity] = useState<UserActivity | null>(null);
  const [feedbackGiven, setFeedbackGiven] = useState<Set<number>>(new Set());
  const [submittingFeedback, setSubmittingFeedback] = useState(false);

  const loadActivities = useCallback(async () => {
    try {
      const data = await usersApi.getMyActivities();
      setActivities(data);
    } catch (error: any) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: error.message || 'Could not load activities',
      });
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadActivities();
  }, [loadActivities]);

  const handleRefresh = () => {
    setRefreshing(true);
    loadActivities();
  };

  const handleActivityPress = (activityId: number) => {
    if (onActivityPress) {
      onActivityPress(activityId);
    } else {
      navigation?.navigate('ActivityDetail', { activityId });
    }
  };

  const handleFeedbackPress = (activity: UserActivity) => {
    setSelectedActivity(activity);
    setFeedbackModalVisible(true);
  };

  const handleFeedbackSubmit = async (feedback: {
    didMeet: boolean;
    experienceRating: number;
    wouldHangOutAgain: boolean;
    addedToContacts: boolean;
    comments?: string;
  }) => {
    if (!selectedActivity) return;

    setSubmittingFeedback(true);
    try {
      await feedbackApi.submit({
        activityId: selectedActivity.id,
        ...feedback,
      });

      setFeedbackGiven((prev) => new Set([...prev, selectedActivity.id]));
      setFeedbackModalVisible(false);
      setSelectedActivity(null);

      Toast.show({
        type: 'success',
        text1: 'Thanks for your feedback!',
        text2: 'Your input helps improve the community.',
      });
    } catch (error: any) {
      if (error.message?.includes('409') || error.message?.includes('Conflict')) {
        Toast.show({
          type: 'info',
          text1: 'Already Submitted',
          text2: 'You have already given feedback for this activity.',
        });
        setFeedbackGiven((prev) => new Set([...prev, selectedActivity.id]));
        setFeedbackModalVisible(false);
      } else {
        Toast.show({
          type: 'error',
          text1: 'Error',
          text2: error.message || 'Could not submit feedback',
        });
      }
    } finally {
      setSubmittingFeedback(false);
    }
  };

  const canGiveFeedback = (activity: UserActivity) => {
    // Can give feedback for completed activities that we haven't given feedback for yet
    return (
      activity.status === 'COMPLETED' &&
      activity.participationStatus === 'CONFIRMED' &&
      !feedbackGiven.has(activity.id)
    );
  };

  const filteredActivities = activities.filter((activity) => {
    const activityDate = new Date(activity.startTime);
    const now = new Date();
    if (filter === 'upcoming') {
      return activityDate >= now || activity.status === 'ACTIVE';
    }
    return activityDate < now && activity.status !== 'ACTIVE';
  });

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    if (date.toDateString() === today.toDateString()) {
      return 'Today';
    }
    if (date.toDateString() === tomorrow.toDateString()) {
      return 'Tomorrow';
    }
    return date.toLocaleDateString([], {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return '#4CAF50';
      case 'COMPLETED':
        return '#9E9E9E';
      case 'CANCELLED':
        return '#F44336';
      default:
        return '#6200EE';
    }
  };

  const renderActivityCard = ({ item }: { item: UserActivity }) => (
    <Card
      style={styles.card}
      onPress={() => handleActivityPress(item.id)}
    >
      <Card.Content>
        <View style={styles.cardHeader}>
          <Chip
            icon={CATEGORY_ICONS[item.category]}
            style={[
              styles.categoryChip,
              { backgroundColor: CATEGORY_COLORS[item.category] + '20' },
            ]}
            textStyle={{ color: CATEGORY_COLORS[item.category] }}
          >
            {item.category}
          </Chip>
          {item.isCreator && (
            <Badge style={styles.creatorBadge}>Creator</Badge>
          )}
        </View>

        <Text variant="titleMedium" style={styles.title}>
          {item.title}
        </Text>

        <View style={styles.detailsRow}>
          <Text variant="bodySmall" style={styles.hubText}>
            {item.hubName}
          </Text>
          <Text variant="bodySmall" style={styles.dot}>•</Text>
          <Text variant="bodySmall" style={styles.timeText}>
            {formatDate(item.startTime)} at {formatTime(item.startTime)}
          </Text>
        </View>

        <View style={styles.footer}>
          <Chip
            style={[
              styles.statusChip,
              { backgroundColor: getStatusColor(item.status) + '20' },
            ]}
            textStyle={{ color: getStatusColor(item.status), fontSize: 12 }}
          >
            {item.status}
          </Chip>
          <Chip
            style={styles.participationChip}
            textStyle={{ fontSize: 12 }}
          >
            {item.participationStatus === 'CONFIRMED' ? 'Going' : 'Interested'}
          </Chip>
          {canGiveFeedback(item) && (
            <Button
              mode="contained"
              compact
              onPress={() => handleFeedbackPress(item)}
              style={styles.feedbackButton}
              labelStyle={styles.feedbackButtonLabel}
            >
              Rate
            </Button>
          )}
          {feedbackGiven.has(item.id) && (
            <Chip
              style={styles.feedbackGivenChip}
              textStyle={{ fontSize: 11, color: '#4CAF50' }}
              icon="check"
            >
              Rated
            </Chip>
          )}
        </View>
      </Card.Content>
    </Card>
  );

  const renderEmptyState = () => (
    <View style={styles.emptyContainer}>
      <Text variant="titleMedium" style={styles.emptyTitle}>
        {filter === 'upcoming'
          ? 'No upcoming activities'
          : 'No past activities'}
      </Text>
      <Text variant="bodyMedium" style={styles.emptySubtitle}>
        {filter === 'upcoming'
          ? 'Join or create an activity to get started!'
          : 'Your completed activities will appear here.'}
      </Text>
    </View>
  );

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" />
        <Text style={styles.loadingText}>Loading your activities...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <SegmentedButtons
        value={filter}
        onValueChange={(value) => setFilter(value as 'upcoming' | 'past')}
        buttons={[
          { value: 'upcoming', label: 'Upcoming' },
          { value: 'past', label: 'Past' },
        ]}
        style={styles.segmentedButtons}
      />

      <FlatList
        data={filteredActivities}
        renderItem={renderActivityCard}
        keyExtractor={(item) => item.id.toString()}
        contentContainerStyle={styles.listContent}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
        }
        ListEmptyComponent={renderEmptyState}
      />

      {selectedActivity && (
        <FeedbackModal
          visible={feedbackModalVisible}
          activity={{
            id: selectedActivity.id,
            title: selectedActivity.title,
            hubId: selectedActivity.hubId,
            hubName: selectedActivity.hubName,
            category: selectedActivity.category,
            startTime: selectedActivity.startTime,
            endTime: selectedActivity.endTime,
            createdBy: 0,
            createdByName: '',
          }}
          onClose={() => {
            setFeedbackModalVisible(false);
            setSelectedActivity(null);
          }}
          onSubmit={handleFeedbackSubmit}
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
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5F5F5',
  },
  loadingText: {
    marginTop: 16,
    color: '#666',
  },
  segmentedButtons: {
    marginHorizontal: 16,
    marginVertical: 12,
  },
  listContent: {
    paddingHorizontal: 16,
    paddingBottom: 16,
  },
  card: {
    marginBottom: 12,
    backgroundColor: '#fff',
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  categoryChip: {
    height: 28,
  },
  creatorBadge: {
    backgroundColor: '#6200EE',
  },
  title: {
    fontWeight: '600',
    marginBottom: 8,
  },
  detailsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  hubText: {
    color: '#666',
  },
  dot: {
    marginHorizontal: 6,
    color: '#666',
  },
  timeText: {
    color: '#666',
  },
  footer: {
    flexDirection: 'row',
    gap: 8,
  },
  statusChip: {
    height: 26,
  },
  participationChip: {
    height: 26,
    backgroundColor: '#E8E8E8',
  },
  feedbackButton: {
    height: 28,
    marginLeft: 'auto',
  },
  feedbackButtonLabel: {
    fontSize: 11,
    marginVertical: 0,
  },
  feedbackGivenChip: {
    height: 26,
    backgroundColor: '#E8F5E9',
    marginLeft: 'auto',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 64,
    paddingHorizontal: 32,
  },
  emptyTitle: {
    color: '#333',
    marginBottom: 8,
    textAlign: 'center',
  },
  emptySubtitle: {
    color: '#666',
    textAlign: 'center',
  },
});
