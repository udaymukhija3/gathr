import React from 'react';
import { View, StyleSheet, TouchableOpacity } from 'react-native';
import { Card, Text, Badge, Button, IconButton } from 'react-native-paper';
import { format, parseISO } from 'date-fns';
import { Activity } from '../types';

interface ActivityCardProps {
  activity: Activity;
  onPress: () => void;
  onJoin?: () => void;
}

const getCategoryColor = (category: string): string => {
  switch (category) {
    case 'SPORTS':
      return '#4CAF50';
    case 'FOOD':
      return '#FF9800';
    case 'ART':
      return '#9C27B0';
    case 'MUSIC':
      return '#2196F3';
    default:
      return '#757575';
  }
};

const getCategoryLabel = (category: string): string => {
  switch (category) {
    case 'SPORTS':
      return 'Sport';
    case 'FOOD':
      return 'Food';
    case 'ART':
      return 'Art';
    case 'MUSIC':
      return 'Music';
    default:
      return category;
  }
};

export const ActivityCard: React.FC<ActivityCardProps> = ({
  activity,
  onPress,
  onJoin,
}) => {
  const startTime = parseISO(activity.startTime);
  const endTime = parseISO(activity.endTime);
  const timeRange = `${format(startTime, 'h:mm a')} - ${format(endTime, 'h:mm a')}`;

  return (
    <Card style={styles.card} onPress={onPress}>
      <Card.Content>
        <View style={styles.header}>
          <Text variant="titleMedium" style={styles.title}>
            {activity.title}
          </Text>
          {activity.isInviteOnly && (
            <IconButton
              icon="lock"
              size={20}
              iconColor="#FF9800"
              style={styles.lockIcon}
            />
          )}
        </View>

        <View style={styles.meta}>
          <Badge style={[styles.badge, { backgroundColor: getCategoryColor(activity.category) }]}>
            {getCategoryLabel(activity.category)}
          </Badge>
          <Text variant="bodySmall" style={styles.hub}>
            {activity.hubName}
          </Text>
        </View>

        <Text variant="bodySmall" style={styles.time}>
          {timeRange}
        </Text>

        <View style={styles.stats}>
          <View style={styles.stat}>
            <Text variant="bodySmall" style={styles.statLabel}>
              👥 {activity.peopleCount || 0}
            </Text>
          </View>
          {activity.mutualsCount !== undefined && activity.mutualsCount > 0 && (
            <View style={styles.stat}>
              <Text variant="bodySmall" style={styles.statLabel}>
                🤝 {activity.mutualsCount} mutuals
              </Text>
            </View>
          )}
        </View>

        {onJoin && (
          <Button
            mode="contained"
            onPress={onJoin}
            style={styles.joinButton}
            disabled={activity.isInviteOnly}
          >
            {activity.isInviteOnly ? 'Request Invite' : 'Join'}
          </Button>
        )}
      </Card.Content>
    </Card>
  );
};

const styles = StyleSheet.create({
  card: {
    marginVertical: 8,
    marginHorizontal: 16,
    elevation: 2,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  title: {
    flex: 1,
    fontWeight: '600',
  },
  lockIcon: {
    margin: 0,
  },
  meta: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
    gap: 8,
  },
  badge: {
    alignSelf: 'flex-start',
  },
  hub: {
    color: '#666',
  },
  time: {
    color: '#666',
    marginBottom: 8,
  },
  stats: {
    flexDirection: 'row',
    gap: 16,
    marginBottom: 12,
  },
  stat: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statLabel: {
    color: '#666',
  },
  joinButton: {
    marginTop: 8,
  },
});

