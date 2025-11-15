import React, { useEffect, useState } from 'react';
import { View, StyleSheet, ScrollView, Alert } from 'react-native';
import { Text, Button, ActivityIndicator, Card, Chip } from 'react-native-paper';
import { format, parseISO } from 'date-fns';
import { ActivityDetail } from '../types';
import { activitiesApi } from '../services/api';
import { AvatarAnon } from '../components/AvatarAnon';
import { MutualBadge } from '../components/MutualBadge';

interface ActivityDetailScreenProps {
  activityId: number;
  onJoinChat: () => void;
  onInvite: () => void;
}

export const ActivityDetailScreen: React.FC<ActivityDetailScreenProps> = ({
  activityId,
  onJoinChat,
  onInvite,
}) => {
  const [activity, setActivity] = useState<ActivityDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [joining, setJoining] = useState(false);

  useEffect(() => {
    loadActivity();
  }, [activityId]);

  const loadActivity = async () => {
    try {
      setLoading(true);
      const data = await activitiesApi.getById(activityId);
      setActivity(data);
    } catch (error) {
      console.error('Error loading activity:', error);
      Alert.alert('Error', 'Failed to load activity details');
    } finally {
      setLoading(false);
    }
  };

  const handleJoin = async () => {
    try {
      setJoining(true);
      await activitiesApi.join(activityId, 'INTERESTED');
      Alert.alert('Success', 'You\'re interested in this activity!');
      await loadActivity();
    } catch (error) {
      Alert.alert('Error', 'Failed to join activity');
    } finally {
      setJoining(false);
    }
  };

  const canJoinChat = () => {
    if (!activity) return false;
    const confirmedCount = activity.participants.filter(
      (p) => p.revealed
    ).length;
    return activity.participants.length >= 3 && confirmedCount >= 1;
  };

  const showSafetyTip = () => {
    if (!activity) return false;
    return activity.participants.length < 2;
  };

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (!activity) {
    return (
      <View style={styles.center}>
        <Text>Activity not found</Text>
      </View>
    );
  }

  const startTime = parseISO(activity.startTime);
  const endTime = parseISO(activity.endTime);
  const timeRange = `${format(startTime, 'MMM d, h:mm a')} - ${format(endTime, 'h:mm a')}`;

  return (
    <ScrollView style={styles.container}>
      <Card style={styles.card}>
        <Card.Content>
          <View style={styles.header}>
            <Text variant="headlineSmall" style={styles.title}>
              {activity.title}
            </Text>
            {activity.isInviteOnly && (
              <Chip icon="lock" style={styles.inviteOnlyChip}>
                Invite Only
              </Chip>
            )}
          </View>

          <Chip style={styles.categoryChip}>{activity.category}</Chip>

          <Text variant="bodyMedium" style={styles.hub}>
            📍 {activity.hubName}
          </Text>

          <Text variant="bodyMedium" style={styles.time}>
            🕐 {timeRange}
          </Text>

          {activity.description && (
            <Text variant="bodyMedium" style={styles.description}>
              {activity.description}
            </Text>
          )}

          <View style={styles.stats}>
            <Text variant="bodyLarge" style={styles.stat}>
              👥 {activity.peopleCount || activity.participants.length} people
            </Text>
            {activity.mutualsCount !== undefined && activity.mutualsCount > 0 && (
              <MutualBadge count={activity.mutualsCount} />
            )}
          </View>
        </Card.Content>
      </Card>

      <Card style={styles.card}>
        <Card.Content>
          <Text variant="titleMedium" style={styles.sectionTitle}>
            Participants
          </Text>
          <View style={styles.participants}>
            {activity.participants.map((participant, index) => (
              <View key={participant.anonId} style={styles.participant}>
                <AvatarAnon
                  name={participant.revealed && participant.name ? participant.name : `Member #${index + 1}`}
                  revealed={participant.revealed}
                />
                {participant.mutualsCount > 0 && (
                  <MutualBadge count={participant.mutualsCount} />
                )}
              </View>
            ))}
          </View>
        </Card.Content>
      </Card>

      {showSafetyTip() && (
        <Card style={[styles.card, styles.safetyCard]}>
          <Card.Content>
            <Text variant="bodyMedium" style={styles.safetyText}>
              💡 Groups form faster with 2+ people. Invite friends to join!
            </Text>
          </Card.Content>
        </Card>
      )}

      <View style={styles.actions}>
        <Button
          mode="contained"
          onPress={handleJoin}
          loading={joining}
          disabled={joining}
          style={styles.button}
        >
          I'm Interested
        </Button>

        {canJoinChat() && (
          <Button
            mode="contained"
            onPress={onJoinChat}
            style={styles.button}
            buttonColor="#4CAF50"
          >
            Join Group Chat
          </Button>
        )}

        <Button mode="outlined" onPress={onInvite} style={styles.button}>
          Invite Friends
        </Button>
      </View>
    </ScrollView>
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
  card: {
    margin: 16,
    marginBottom: 8,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 12,
  },
  title: {
    flex: 1,
    fontWeight: 'bold',
    marginRight: 8,
  },
  inviteOnlyChip: {
    alignSelf: 'flex-start',
  },
  categoryChip: {
    alignSelf: 'flex-start',
    marginBottom: 8,
  },
  hub: {
    marginTop: 8,
    marginBottom: 4,
  },
  time: {
    marginBottom: 8,
  },
  description: {
    marginTop: 8,
    marginBottom: 12,
    color: '#666',
  },
  stats: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginTop: 8,
  },
  stat: {
    fontWeight: '600',
  },
  sectionTitle: {
    marginBottom: 16,
    fontWeight: '600',
  },
  participants: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  participant: {
    alignItems: 'center',
    marginRight: 16,
    marginBottom: 16,
  },
  safetyCard: {
    backgroundColor: '#FFF3E0',
  },
  safetyText: {
    color: '#E65100',
  },
  actions: {
    padding: 16,
    gap: 12,
  },
  button: {
    marginBottom: 8,
  },
});

