import React, { useCallback, useEffect, useState } from 'react';
import { View, StyleSheet, FlatList, RefreshControl } from 'react-native';
import { ActivityIndicator, Button, Card, Chip, Text } from 'react-native-paper';
import Toast from 'react-native-toast-message';
import { usersApi } from '../services/api';
import { UserActivity } from '../types';

interface ChatsScreenProps {
  navigation?: any;
  onOpenChat?: (activityId: number) => void;
}

export const ChatsScreen: React.FC<ChatsScreenProps> = ({
  navigation,
  onOpenChat,
}) => {
  const [activities, setActivities] = useState<UserActivity[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const loadActivities = useCallback(async () => {
    try {
      const data = await usersApi.getMyActivities();
      setActivities(data);
    } catch (error: any) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: error.message || 'Could not load your chats',
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

  const handleOpenChat = (activityId: number) => {
    if (onOpenChat) {
      onOpenChat(activityId);
    } else {
      navigation?.navigate('Chat', { activityId });
    }
  };

  const formatDateTime = (iso: string) => {
    const date = new Date(iso);
    return date.toLocaleString([], {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const renderChatCard = ({ item }: { item: UserActivity }) => (
    <Card style={styles.card} onPress={() => handleOpenChat(item.id)}>
      <Card.Content>
        <View style={styles.cardHeader}>
          <Text variant="titleMedium" style={styles.title}>
            {item.title}
          </Text>
          {item.isCreator && (
            <Chip compact icon="crown" style={styles.creatorChip}>
              Host
            </Chip>
          )}
        </View>

        <View style={styles.metaRow}>
          {item.hubName && (
            <Chip icon="map-marker" compact style={styles.metaChip}>
              {item.hubName}
            </Chip>
          )}
          <Chip icon="clock" compact style={styles.metaChip}>
            {formatDateTime(item.startTime)}
          </Chip>
        </View>

        <View style={styles.footerRow}>
          <Chip compact style={styles.statusChip}>
            {item.participationStatus === 'CONFIRMED' ? 'Going' : 'Interested'}
          </Chip>
          <Chip compact style={styles.statusChip}>
            {item.status}
          </Chip>
          <Button
            mode="contained-tonal"
            onPress={() => handleOpenChat(item.id)}
            style={styles.chatButton}
            icon="message-text"
          >
            Open chat
          </Button>
        </View>
      </Card.Content>
    </Card>
  );

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" />
        <Text style={styles.loadingText}>Loading your chats...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text variant="headlineSmall" style={styles.headerTitle}>
          Chat with your groups
        </Text>
        <Text variant="bodyMedium" style={styles.headerSubtitle}>
          Pick an activity to jump back into the conversation.
        </Text>
      </View>

      <FlatList
        data={activities}
        keyExtractor={(item) => item.id.toString()}
        renderItem={renderChatCard}
        contentContainerStyle={styles.listContent}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
        }
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text variant="titleMedium" style={styles.emptyTitle}>
              No chats yet
            </Text>
            <Text variant="bodyMedium" style={styles.emptySubtitle}>
              Join or create an activity to unlock chat.
            </Text>
          </View>
        }
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  header: {
    paddingHorizontal: 16,
    paddingVertical: 20,
    backgroundColor: '#fff',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#E0E0E0',
  },
  headerTitle: {
    fontWeight: 'bold',
    color: '#333',
  },
  headerSubtitle: {
    marginTop: 6,
    color: '#666',
  },
  listContent: {
    padding: 16,
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
  title: {
    flex: 1,
    marginRight: 12,
    fontWeight: '600',
  },
  metaRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 12,
  },
  metaChip: {
    backgroundColor: '#F2F2F2',
  },
  statusChip: {
    backgroundColor: '#EFE7FD',
    marginRight: 8,
  },
  footerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: 8,
  },
  chatButton: {
    marginLeft: 'auto',
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5F5F5',
  },
  loadingText: {
    marginTop: 12,
    color: '#666',
  },
  emptyState: {
    marginTop: 60,
    alignItems: 'center',
    paddingHorizontal: 16,
  },
  emptyTitle: {
    marginBottom: 8,
    color: '#333',
  },
  emptySubtitle: {
    color: '#666',
    textAlign: 'center',
  },
  creatorChip: {
    backgroundColor: '#E3F2FD',
  },
});


