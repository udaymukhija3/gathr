import React, { useEffect, useState } from 'react';
import { View, StyleSheet, ScrollView } from 'react-native';
import { Text, Button, ActivityIndicator, Card, Chip } from 'react-native-paper';
import { format, parseISO } from 'date-fns';
import Toast from 'react-native-toast-message';
import { ActivityDetail } from '../types';
import { activitiesApi } from '../services/api';
import { AvatarAnon } from '../components/AvatarAnon';
import { MutualBadge } from '../components/MutualBadge';
import { InviteModal } from '../components/InviteModal';
import { trackActivity } from '../utils/telemetry';

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
  const [showInviteModal, setShowInviteModal] = useState(false);

  useEffect(() => {
    loadActivity();
  }, [activityId]);

  const loadActivity = async () => {
    try {
      setLoading(true);
      const data = await activitiesApi.getById(activityId);
      setActivity(data);

      // Track activity detail viewed
      trackActivity.viewed(activityId, {
        participantCount: data.participants.length,
        isInviteOnly: data.isInviteOnly,
        mutualsCount: data.mutualsCount,
      });
    } catch (error) {
      console.error('Error loading activity:', error);
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Failed to load activity details',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleJoin = async (inviteToken?: string) => {
    // Check if invite-only and no token provided
    if (activity?.isInviteOnly && !inviteToken) {
      trackActivity.inviteModalOpened(activityId, { source: 'detail' });
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
      setJoining(true);
      await activitiesApi.join(activityId, 'INTERESTED', inviteToken);
      trackActivity.joined(activityId, 'INTERESTED', {
        source: 'detail',
        usedInviteToken: !!inviteToken,
      });
      Toast.show({
        type: 'success',
        text1: 'Success',
        text2: 'You\'re interested in this activity!',
      });
      await loadActivity();
      setShowInviteModal(false);
    } catch (error: any) {
      const errorMessage = error.message || 'Failed to join activity';
      if (errorMessage.includes('403') || errorMessage.includes('Forbidden')) {
        trackActivity.inviteModalOpened(activityId, { source: 'detail', reason: 'forbidden' });
        Toast.show({
          type: 'error',
          text1: 'Invite Required',
          text2: 'This is an invite-only activity. Please enter a valid invite token.',
        });
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
  const locationName = activity.locationName || activity.hubName;

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

          {locationName && (
            <Text variant="bodyMedium" style={styles.hub}>
              📍 {locationName}
            </Text>
          )}
          {activity.locationAddress && (
            <Text variant="bodySmall" style={styles.address}>
              {activity.locationAddress}
            </Text>
          )}

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
            onPress={() => {
              trackActivity.chatJoined(activityId, { source: 'detail' });
              onJoinChat();
            }}
            style={styles.button}
            buttonColor="#4CAF50"
          >
            Join Group Chat
          </Button>
        )}

        <Button
          mode="outlined"
          onPress={() => {
            trackActivity.inviteModalOpened(activityId, { source: 'detail_invite_button' });
            onInvite();
          }}
          style={styles.button}
        >
          Invite Friends
        </Button>
      </View>

      <InviteModal
        visible={showInviteModal}
        activityId={activityId}
        onClose={() => setShowInviteModal(false)}
        onTokenEntered={(token) => handleJoin(token)}
      />
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
  address: {
    color: '#666',
    marginBottom: 8,
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

