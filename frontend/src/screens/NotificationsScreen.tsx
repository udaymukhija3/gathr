import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, FlatList, RefreshControl, TouchableOpacity } from 'react-native';
import {
  Text,
  Card,
  ActivityIndicator,
  IconButton,
  Badge,
  Divider,
} from 'react-native-paper';
import { formatDistanceToNow } from 'date-fns';
import Toast from 'react-native-toast-message';
import { notificationsApi } from '../services/api';

interface Notification {
  id: number;
  type: string;
  title: string;
  message: string;
  activityId?: number;
  createdAt: string;
  readAt: string | null;
}

const NOTIFICATION_ICONS: Record<string, string> = {
  ACTIVITY_REMINDER: 'bell-ring',
  NEW_MESSAGE: 'message-text',
  ACTIVITY_JOINED: 'account-plus',
  ACTIVITY_CONFIRMED: 'check-circle',
  ACTIVITY_CANCELLED: 'cancel',
  PROMOTIONAL: 'tag',
  DEFAULT: 'bell',
};

const NOTIFICATION_COLORS: Record<string, string> = {
  ACTIVITY_REMINDER: '#FFC107',
  NEW_MESSAGE: '#2196F3',
  ACTIVITY_JOINED: '#4CAF50',
  ACTIVITY_CONFIRMED: '#4CAF50',
  ACTIVITY_CANCELLED: '#F44336',
  PROMOTIONAL: '#9C27B0',
  DEFAULT: '#6200EE',
};

interface NotificationsScreenProps {
  navigation?: any;
  onNotificationPress?: (notification: Notification) => void;
}

export const NotificationsScreen: React.FC<NotificationsScreenProps> = ({
  navigation,
  onNotificationPress,
}) => {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const loadNotifications = useCallback(async () => {
    try {
      const data = await notificationsApi.getUnread();
      setNotifications(data);
    } catch (error: any) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: error.message || 'Could not load notifications',
      });
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  const handleRefresh = () => {
    setRefreshing(true);
    loadNotifications();
  };

  const handleNotificationPress = async (notification: Notification) => {
    // Mark as read
    try {
      await notificationsApi.markAsRead(notification.id);
      setNotifications((prev) =>
        prev.map((n) =>
          n.id === notification.id ? { ...n, readAt: new Date().toISOString() } : n
        )
      );
    } catch (error) {
      console.error('Error marking notification as read:', error);
    }

    // Navigate based on notification type
    if (onNotificationPress) {
      onNotificationPress(notification);
    } else if (notification.activityId) {
      navigation?.navigate('ActivityDetail', { activityId: notification.activityId });
    }
  };

  const getIcon = (type: string) => NOTIFICATION_ICONS[type] || NOTIFICATION_ICONS.DEFAULT;
  const getColor = (type: string) => NOTIFICATION_COLORS[type] || NOTIFICATION_COLORS.DEFAULT;

  const renderNotification = ({ item }: { item: Notification }) => {
    const isUnread = !item.readAt;
    const timeAgo = formatDistanceToNow(new Date(item.createdAt), { addSuffix: true });

    return (
      <TouchableOpacity onPress={() => handleNotificationPress(item)}>
        <Card style={[styles.card, isUnread && styles.unreadCard]}>
          <Card.Content style={styles.cardContent}>
            <View
              style={[
                styles.iconContainer,
                { backgroundColor: getColor(item.type) + '20' },
              ]}
            >
              <IconButton
                icon={getIcon(item.type)}
                iconColor={getColor(item.type)}
                size={24}
              />
            </View>
            <View style={styles.textContainer}>
              <View style={styles.titleRow}>
                <Text
                  variant="titleSmall"
                  style={[styles.title, isUnread && styles.unreadText]}
                  numberOfLines={1}
                >
                  {item.title}
                </Text>
                {isUnread && <Badge size={8} style={styles.unreadBadge} />}
              </View>
              <Text variant="bodyMedium" style={styles.message} numberOfLines={2}>
                {item.message}
              </Text>
              <Text variant="bodySmall" style={styles.time}>
                {timeAgo}
              </Text>
            </View>
          </Card.Content>
        </Card>
      </TouchableOpacity>
    );
  };

  const renderEmptyState = () => (
    <View style={styles.emptyContainer}>
      <IconButton icon="bell-off-outline" size={64} iconColor="#CCC" />
      <Text variant="titleMedium" style={styles.emptyTitle}>
        No notifications
      </Text>
      <Text variant="bodyMedium" style={styles.emptySubtitle}>
        You're all caught up! We'll notify you when something happens.
      </Text>
    </View>
  );

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" />
        <Text style={styles.loadingText}>Loading notifications...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={notifications}
        renderItem={renderNotification}
        keyExtractor={(item) => item.id.toString()}
        contentContainerStyle={styles.listContent}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
        }
        ListEmptyComponent={renderEmptyState}
        ItemSeparatorComponent={() => <View style={styles.separator} />}
      />
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
  listContent: {
    paddingVertical: 8,
    flexGrow: 1,
  },
  card: {
    marginHorizontal: 16,
    marginVertical: 4,
    backgroundColor: '#fff',
  },
  unreadCard: {
    backgroundColor: '#F3E5F5',
  },
  cardContent: {
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  iconContainer: {
    borderRadius: 24,
    marginRight: 8,
  },
  textContainer: {
    flex: 1,
    paddingTop: 8,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  title: {
    fontWeight: '600',
    flex: 1,
  },
  unreadText: {
    fontWeight: 'bold',
  },
  unreadBadge: {
    backgroundColor: '#6200EE',
    marginLeft: 8,
  },
  message: {
    color: '#666',
    marginTop: 2,
  },
  time: {
    color: '#999',
    marginTop: 4,
  },
  separator: {
    height: 4,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 32,
    paddingVertical: 64,
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
