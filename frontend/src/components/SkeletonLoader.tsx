import React, { useEffect, useRef } from 'react';
import { View, StyleSheet, Animated, Dimensions } from 'react-native';

const { width } = Dimensions.get('window');

interface SkeletonProps {
  width?: number | string;
  height?: number;
  borderRadius?: number;
  style?: any;
}

export const Skeleton: React.FC<SkeletonProps> = ({
  width: skeletonWidth = '100%',
  height = 20,
  borderRadius = 4,
  style,
}) => {
  const shimmerAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const animation = Animated.loop(
      Animated.sequence([
        Animated.timing(shimmerAnim, {
          toValue: 1,
          duration: 1000,
          useNativeDriver: true,
        }),
        Animated.timing(shimmerAnim, {
          toValue: 0,
          duration: 1000,
          useNativeDriver: true,
        }),
      ])
    );
    animation.start();
    return () => animation.stop();
  }, []);

  const opacity = shimmerAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [0.3, 0.7],
  });

  return (
    <Animated.View
      style={[
        styles.skeleton,
        {
          width: skeletonWidth,
          height,
          borderRadius,
          opacity,
        },
        style,
      ]}
    />
  );
};

// Activity Card Skeleton
export const ActivityCardSkeleton: React.FC = () => (
  <View style={styles.cardContainer}>
    <View style={styles.cardContent}>
      {/* Category chip */}
      <Skeleton width={80} height={24} borderRadius={12} />

      {/* Title */}
      <Skeleton width="70%" height={22} style={styles.marginTop12} />

      {/* Hub and time row */}
      <View style={styles.row}>
        <Skeleton width={100} height={16} />
        <Skeleton width={80} height={16} style={styles.marginLeft8} />
      </View>

      {/* People count and mutuals */}
      <View style={styles.row}>
        <Skeleton width={60} height={16} />
        <Skeleton width={100} height={16} style={styles.marginLeft8} />
      </View>

      {/* Join button */}
      <Skeleton width={100} height={36} borderRadius={18} style={styles.marginTop12} />
    </View>
  </View>
);

// Feed Screen Skeleton (multiple cards)
export const FeedSkeleton: React.FC<{ count?: number }> = ({ count = 3 }) => (
  <View style={styles.feedContainer}>
    {/* Hub selector skeleton */}
    <View style={styles.hubSelectorSkeleton}>
      <Skeleton width={80} height={32} borderRadius={16} />
      <Skeleton width={80} height={32} borderRadius={16} style={styles.marginLeft8} />
      <Skeleton width={80} height={32} borderRadius={16} style={styles.marginLeft8} />
    </View>

    {/* Activity cards */}
    {Array.from({ length: count }).map((_, index) => (
      <ActivityCardSkeleton key={index} />
    ))}
  </View>
);

// Profile Skeleton
export const ProfileSkeleton: React.FC = () => (
  <View style={styles.profileContainer}>
    {/* Avatar */}
    <View style={styles.avatarContainer}>
      <Skeleton width={80} height={80} borderRadius={40} />
    </View>

    {/* Name and phone */}
    <View style={styles.profileInfo}>
      <Skeleton width={150} height={24} />
      <Skeleton width={120} height={16} style={styles.marginTop8} />
    </View>

    {/* Cards */}
    <View style={styles.profileCard}>
      <Skeleton width={60} height={18} />
      <Skeleton width="100%" height={60} style={styles.marginTop12} />
    </View>

    <View style={styles.profileCard}>
      <Skeleton width={80} height={18} />
      <View style={styles.interestsRow}>
        <Skeleton width={70} height={32} borderRadius={16} />
        <Skeleton width={70} height={32} borderRadius={16} style={styles.marginLeft8} />
        <Skeleton width={70} height={32} borderRadius={16} style={styles.marginLeft8} />
      </View>
    </View>
  </View>
);

// Chat Message Skeleton
export const ChatMessageSkeleton: React.FC<{ isOwn?: boolean }> = ({ isOwn = false }) => (
  <View style={[styles.messageContainer, isOwn && styles.messageOwn]}>
    {!isOwn && <Skeleton width={32} height={32} borderRadius={16} />}
    <View style={[styles.messageBubble, isOwn && styles.messageBubbleOwn]}>
      <Skeleton width={isOwn ? 120 : 180} height={16} />
      <Skeleton width={isOwn ? 80 : 140} height={16} style={styles.marginTop4} />
    </View>
  </View>
);

// Chat Skeleton
export const ChatSkeleton: React.FC = () => (
  <View style={styles.chatContainer}>
    <ChatMessageSkeleton />
    <ChatMessageSkeleton isOwn />
    <ChatMessageSkeleton />
    <ChatMessageSkeleton isOwn />
    <ChatMessageSkeleton />
  </View>
);

const styles = StyleSheet.create({
  skeleton: {
    backgroundColor: '#E0E0E0',
  },
  cardContainer: {
    backgroundColor: '#fff',
    marginHorizontal: 16,
    marginVertical: 8,
    borderRadius: 12,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  cardContent: {
    padding: 16,
  },
  marginTop4: {
    marginTop: 4,
  },
  marginTop8: {
    marginTop: 8,
  },
  marginTop12: {
    marginTop: 12,
  },
  marginLeft8: {
    marginLeft: 8,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 12,
  },
  feedContainer: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  hubSelectorSkeleton: {
    flexDirection: 'row',
    padding: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  profileContainer: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  avatarContainer: {
    alignItems: 'center',
    paddingVertical: 24,
    backgroundColor: '#6200EE',
  },
  profileInfo: {
    alignItems: 'center',
    paddingVertical: 16,
    backgroundColor: '#fff',
  },
  profileCard: {
    backgroundColor: '#fff',
    marginHorizontal: 16,
    marginTop: 12,
    padding: 16,
    borderRadius: 8,
  },
  interestsRow: {
    flexDirection: 'row',
    marginTop: 12,
  },
  messageContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    marginVertical: 4,
    marginHorizontal: 16,
  },
  messageOwn: {
    justifyContent: 'flex-end',
  },
  messageBubble: {
    backgroundColor: '#E8E8E8',
    padding: 12,
    borderRadius: 16,
    marginLeft: 8,
  },
  messageBubbleOwn: {
    backgroundColor: '#E3F2FD',
    marginLeft: 0,
    marginRight: 8,
  },
  chatContainer: {
    flex: 1,
    backgroundColor: '#F5F5F5',
    paddingTop: 16,
  },
});
