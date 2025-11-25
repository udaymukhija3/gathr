import React from 'react';
import { View, StyleSheet, TouchableOpacity } from 'react-native';
import { Card, Text, Badge, Button, IconButton } from 'react-native-paper';
import { format, parseISO } from 'date-fns';
import { Activity } from '../types';

interface ActivityCardProps {
  activity: Activity;
  onPress: () => void;
  onJoin?: () => void;
  isJoined?: boolean;
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
    case 'OUTDOOR':
      return '#8BC34A';
    case 'GAMES':
      return '#FF5722';
    case 'LEARNING':
      return '#795548';
    case 'WELLNESS':
      return '#00BCD4';
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
    case 'OUTDOOR':
      return 'Outdoors';
    case 'GAMES':
      return 'Games';
    case 'LEARNING':
      return 'Learning';
    case 'WELLNESS':
      return 'Wellness';
    default:
      return category;
  }
};

export const ActivityCard: React.FC<ActivityCardProps> = ({
  activity,
  onPress,
  onJoin,
  isJoined = false,
}) => {
  const startTime = parseISO(activity.startTime);
  const endTime = parseISO(activity.endTime);
  const timeRange = `${format(startTime, 'h:mm a')} - ${format(endTime, 'h:mm a')}`;
  const locationLabel = activity.locationName || activity.hubName || 'Custom location';
  const distanceLabel =
    activity.distanceKm !== undefined
      ? `${activity.distanceKm < 1 ? `${Math.round(activity.distanceKm * 1000)}m` : `${activity.distanceKm.toFixed(1)} km`} away`
      : undefined;

  const currentCount = activity.peopleCount || 0;
  const maxMembers = activity.maxMembers;
  const isFull = maxMembers !== undefined && currentCount >= maxMembers;
  const isAlmostFull = maxMembers !== undefined && currentCount >= maxMembers - 2 && !isFull;
  const derivedSpotsLeft =
    activity.spotsRemaining !== undefined && activity.spotsRemaining !== null
      ? activity.spotsRemaining
      : maxMembers !== undefined
        ? maxMembers - currentCount
        : null;
  const spotsLeft = derivedSpotsLeft !== null ? Math.max(derivedSpotsLeft, 0) : null;
  const coldStartLabel = activity.coldStartType
    ? activity.coldStartType
        .toLowerCase()
        .split('_')
        .map(part => part.charAt(0).toUpperCase() + part.slice(1))
        .join(' ')
    : undefined;

  return (
    <Card style={styles.card} onPress={onPress}>
      <Card.Content>
        <View style={styles.header}>
          <Text variant="titleMedium" style={styles.title}>
            {activity.title}
          </Text>
          {activity.isNewActivity && (
            <Badge style={styles.newBadge}>New</Badge>
          )}
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
            {locationLabel}
          </Text>
        </View>

        {activity.locationAddress && (
          <Text variant="bodySmall" style={styles.address}>
            {activity.locationAddress}
            {distanceLabel ? ` · ${distanceLabel}` : ''}
          </Text>
        )}
        {!activity.locationAddress && distanceLabel && (
          <Text variant="bodySmall" style={styles.address}>
            {distanceLabel}
          </Text>
        )}

        <Text variant="bodySmall" style={styles.time}>
          {timeRange}
        </Text>

        <View style={styles.stats}>
          <View style={styles.stat}>
            <Text variant="bodySmall" style={styles.statLabel}>
              👥 {currentCount}{maxMembers ? `/${maxMembers}` : ''}
            </Text>
          </View>
          {spotsLeft !== null && spotsLeft > 0 && spotsLeft <= 3 && (
            <Badge style={styles.spotsLeftBadge}>
              {spotsLeft} {spotsLeft === 1 ? 'spot' : 'spots'} left
            </Badge>
          )}
          {isFull && (
            <Badge style={styles.fullBadge}>Full</Badge>
          )}
          {activity.mutualsCount !== undefined && activity.mutualsCount > 0 && (
            <View style={styles.stat}>
              <Text variant="bodySmall" style={styles.statLabel}>
                🤝 {activity.mutualsCount} mutuals
              </Text>
            </View>
          )}
        </View>

        {isJoined && (
          <View style={styles.joinedBadge}>
            <IconButton icon="check-circle" size={16} iconColor="#4CAF50" style={styles.joinedIcon} />
            <Text variant="bodySmall" style={styles.joinedText}>You've joined</Text>
          </View>
        )}

        {(activity.feedPrimaryReason || activity.feedSecondaryReason) && (
          <View style={styles.reasonContainer}>
            {coldStartLabel && (
              <Text variant="bodySmall" style={styles.reasonTag}>
                {coldStartLabel}
              </Text>
            )}
            {activity.feedPrimaryReason && (
              <Text variant="bodySmall" style={styles.reasonPrimary}>
                {activity.feedPrimaryReason}
              </Text>
            )}
            {activity.feedSecondaryReason && (
              <Text variant="bodySmall" style={styles.reasonSecondary}>
                {activity.feedSecondaryReason}
              </Text>
            )}
          </View>
        )}

        {onJoin && !isJoined && (
          <>
            {activity.isInviteOnly ? (
              <Button
                mode="outlined"
                onPress={onJoin}
                style={styles.joinButton}
                icon="lock"
              >
                Request Invite
              </Button>
            ) : (
              <Button
                mode="contained"
                onPress={onJoin}
                style={styles.joinButton}
                disabled={isFull}
              >
                {isFull ? 'Full' : "I'm Interested"}
              </Button>
            )}
            {isFull && (
              <Text variant="bodySmall" style={styles.fullText}>
                This activity is full
              </Text>
            )}
            {activity.isInviteOnly && (
              <Text variant="bodySmall" style={styles.inviteText}>
                This is an invite-only activity
              </Text>
            )}
          </>
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
  newBadge: {
    backgroundColor: '#FF5252',
    color: '#fff',
    marginRight: 4,
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
    color: '#555',
  },
  address: {
    color: '#777',
    marginBottom: 8,
  },
  time: {
    color: '#555',
    marginBottom: 8,
  },
  stats: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginBottom: 12,
    flexWrap: 'wrap',
  },
  stat: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statLabel: {
    color: '#555',
  },
  spotsLeftBadge: {
    backgroundColor: '#FFF3E0',
    color: '#E65100',
  },
  fullBadge: {
    backgroundColor: '#FFEBEE',
    color: '#B00020',
  },
  joinedBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#E8F5E9',
    paddingVertical: 4,
    paddingHorizontal: 12,
    borderRadius: 16,
    alignSelf: 'flex-start',
    marginBottom: 8,
  },
  joinedIcon: {
    margin: 0,
    marginRight: -4,
  },
  joinedText: {
    color: '#2E7D32',
    fontWeight: '500',
  },
  joinButton: {
    marginTop: 8,
  },
  fullText: {
    color: '#B00020',
    marginTop: 4,
    textAlign: 'center',
  },
  inviteText: {
    color: '#555',
    marginTop: 4,
    textAlign: 'center',
    fontStyle: 'italic',
  },
  reasonContainer: {
    backgroundColor: '#F5F5F5',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
    marginBottom: 8,
    gap: 2,
  },
  reasonTag: {
    color: '#6200EE',
    fontWeight: '600',
    textTransform: 'capitalize',
  },
  reasonPrimary: {
    color: '#424242',
    fontWeight: '600',
  },
  reasonSecondary: {
    color: '#616161',
  },
});

