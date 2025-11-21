import React, { useEffect, useState, useRef } from 'react';
import { View, StyleSheet, FlatList, KeyboardAvoidingView, Platform } from 'react-native';
import { TextInput, Button, Text, ActivityIndicator, Card, IconButton } from 'react-native-paper';
import { format, parseISO, differenceInMinutes, differenceInHours, differenceInMilliseconds } from 'date-fns';
import { Message, ActivityDetail } from '../types';
import { messagesApi, activitiesApi, reportsApi } from '../services/api';
import { ChatBubble } from '../components/ChatBubble';
import { ReportModal } from '../components/ReportModal';
import { useWebSocket, WebSocketMessage } from '../hooks/useWebSocket';
import { useUser } from '../context/UserContext';
import Toast from 'react-native-toast-message';
import { trackChat, trackActivity, trackReport } from '../utils/telemetry';

interface ChatScreenProps {
  activityId: number;
  navigation?: any;
}

export const ChatScreen: React.FC<ChatScreenProps> = ({ activityId, navigation }) => {
  const { user } = useUser();
  const [activity, setActivity] = useState<ActivityDetail | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [messageText, setMessageText] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [showReportModal, setShowReportModal] = useState(false);
  const [reportTargetUserId, setReportTargetUserId] = useState<number | null>(null);
  const flatListRef = useRef<FlatList>(null);
  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const lastMessageTimeRef = useRef<string>('');
  const hasTrackedOpen = useRef(false);

  // WebSocket integration
  const { isConnected, sendMessage } = useWebSocket({
    activityId,
    onMessage: (msg: WebSocketMessage) => {
      if (msg.type === 'message') {
        setMessages(prev => {
          // Avoid duplicates
          const exists = prev.find(m => m.id === msg.payload.id);
          if (exists) return prev;
          return [...prev, msg.payload];
        });
        setTimeout(() => flatListRef.current?.scrollToEnd({ animated: true }), 100);
      } else if (msg.type === 'join' || msg.type === 'leave') {
        loadActivity(); // Refresh activity to update participant count
      } else if (msg.type === 'heading_now') {
        loadActivity();
      } else if (msg.type === 'reveal') {
        loadActivity(); // Refresh to show revealed identities
      }
    },
    enabled: true,
  });

  useEffect(() => {
    loadActivity();
    loadMessages(true); // Initial load
    
    return () => {
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
      }
    };
  }, [activityId]);

  // Start/stop polling based on WebSocket connection
  useEffect(() => {
    if (!isConnected) {
      trackChat.websocketDisconnected(activityId);
      startPolling();
    } else {
      trackChat.websocketConnected(activityId);
      // Stop polling when WebSocket is connected
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
        pollIntervalRef.current = null;
      }
    }
  }, [isConnected]);

  const loadActivity = async () => {
    try {
      const data = await activitiesApi.getById(activityId);
      setActivity(data);

      // Track chat opened (only once)
      if (!hasTrackedOpen.current) {
        trackChat.opened(activityId, {
          participantCount: data.participants.length,
          messageCount: data.messagesCount || 0,
        });
        hasTrackedOpen.current = true;
      }
    } catch (error) {
      console.error('Error loading activity:', error);
    }
  };

  const loadMessages = async (initialLoad = false) => {
    try {
      const since = initialLoad ? undefined : lastMessageTimeRef.current || undefined;
      const data = await messagesApi.getByActivity(activityId, since);
      
      if (initialLoad) {
        // Initial load - replace all messages
        setMessages(data.sort((a, b) => 
          new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        ));
        if (data.length > 0) {
          lastMessageTimeRef.current = data[data.length - 1].createdAt;
        }
        setTimeout(() => flatListRef.current?.scrollToEnd({ animated: false }), 100);
      } else if (data.length > 0) {
        // Incremental load - add new messages
        setMessages(prev => {
          const newMessages = data.filter(newMsg => 
            !prev.find(existing => existing.id === newMsg.id)
          );
          const updated = [...prev, ...newMessages].sort((a, b) => 
            new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
          );
          if (updated.length > 0) {
            lastMessageTimeRef.current = updated[updated.length - 1].createdAt;
          }
          return updated;
        });
        setTimeout(() => flatListRef.current?.scrollToEnd({ animated: true }), 100);
      }
      
      setLoading(false);
    } catch (error) {
      console.error('Error loading messages:', error);
      setLoading(false);
    }
  };

  const startPolling = () => {
    // Poll for new messages every 3 seconds (fallback when WebSocket unavailable)
    if (pollIntervalRef.current) {
      clearInterval(pollIntervalRef.current);
    }
    pollIntervalRef.current = setInterval(() => {
      if (!isConnected) {
        loadMessages(false); // Incremental load
      }
    }, 3000);
  };

  const handleSend = async () => {
    if (!messageText.trim()) return;

    const text = messageText.trim();
    setMessageText('');
    setSending(true);

    try {
      // Try WebSocket first, fallback to REST
      if (isConnected) {
        const sent = sendMessage({
          type: 'message',
          payload: { text, activityId },
        });
        if (!sent) {
          // WebSocket failed, use REST
          const newMessage = await messagesApi.create(activityId, text);
          setMessages(prev => [...prev, newMessage].sort((a, b) =>
            new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
          ));
          setTimeout(() => flatListRef.current?.scrollToEnd({ animated: true }), 100);
        }
        trackChat.messageSent(activityId, text.length, { method: 'websocket' });
      } else {
        // Use REST API
        const newMessage = await messagesApi.create(activityId, text);
        setMessages(prev => [...prev, newMessage].sort((a, b) =>
          new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        ));
        setTimeout(() => flatListRef.current?.scrollToEnd({ animated: true }), 100);
        trackChat.messageSent(activityId, text.length, { method: 'rest' });
      }
    } catch (error: any) {
      console.error('Error sending message:', error);
      trackChat.messageFailed(activityId, error.message || 'Unknown error');
      setMessageText(text); // Restore message on error
      Toast.show({
        type: 'error',
        text1: 'Failed to send',
        text2: error.message || 'Please try again',
      });
    } finally {
      setSending(false);
    }
  };

  const handleHeadingThere = async () => {
    trackChat.headingThereClicked(activityId);
    try {
      // Use confirm endpoint
      await activitiesApi.confirm(activityId);
      trackActivity.confirmed(activityId);
      await loadActivity();
      // Send a system message
      await messagesApi.create(activityId, 'Heading there now! 🚶‍♂️');
      if (!isConnected) {
        await loadMessages();
      }
    } catch (error: any) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: error.message || 'Failed to confirm',
      });
    }
  };

  const handleReport = (userId: number) => {
    trackChat.reportClicked(activityId, userId);
    setReportTargetUserId(userId);
    setShowReportModal(true);
  };

  const submitReport = async (reason: string) => {
    if (!reportTargetUserId) return;

    try {
      await reportsApi.create(reportTargetUserId, activityId, reason);
      trackReport.created(reportTargetUserId, activityId, reason);
      Toast.show({
        type: 'success',
        text1: 'Report Submitted',
        text2: 'Thank you for keeping the community safe',
      });
      setShowReportModal(false);
      setReportTargetUserId(null);
    } catch (error: any) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: error.message || 'Failed to submit report',
      });
    }
  };

  const getExpiryCountdown = () => {
    if (!activity) return '';
    const endTime = parseISO(activity.endTime);
    const expiryTime = new Date(endTime.getTime() + 24 * 60 * 60 * 1000);
    const now = new Date();
    const diffMs = expiryTime.getTime() - now.getTime();
    
    if (diffMs < 0) return 'Messages expired';
    
    const hours = Math.floor(diffMs / (60 * 60 * 1000));
    const minutes = Math.floor((diffMs % (60 * 60 * 1000)) / (60 * 1000));
    
    if (hours > 0) {
      return `Messages expire in ${hours}h ${minutes}m`;
    }
    return `Messages expire in ${minutes}m`;
  };

  const getTimeUntilStart = () => {
    if (!activity) return '';
    const start = parseISO(activity.startTime);
    const now = new Date();
    const minutes = differenceInMinutes(start, now);
    if (minutes < 0) return 'Started';
    if (minutes < 60) return `${minutes}m until start`;
    const hours = Math.floor(minutes / 60);
    return `${hours}h ${minutes % 60}m until start`;
  };

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={styles.container}
      keyboardVerticalOffset={90}
    >
      {activity && (
        <Card style={styles.headerCard}>
          <Card.Content>
            <View style={styles.headerRow}>
              <View style={styles.headerContent}>
                <Text variant="titleMedium" style={styles.activityTitle}>
                  {activity.title}
                </Text>
                <View style={styles.meta}>
                  <Text variant="bodySmall">
                    👥 {activity.peopleCount || activity.participants.length} members
                  </Text>
                  <Text variant="bodySmall">🤝 {activity.mutualsCount || 0} mutuals</Text>
                  <Text variant="bodySmall">{getTimeUntilStart()}</Text>
                </View>
              </View>
              <IconButton
                icon="flag"
                size={20}
                onPress={() => {
                  // Report the activity creator or show participant list
                  if (activity.createdBy) {
                    handleReport(activity.createdBy);
                  }
                }}
              />
            </View>
          </Card.Content>
        </Card>
      )}

      <FlatList
        ref={flatListRef}
        data={messages}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => (
          <ChatBubble 
            message={item} 
            isOwn={item.userId === user?.id}
            onReport={() => handleReport(item.userId)}
          />
        )}
        contentContainerStyle={styles.messagesList}
        onContentSizeChange={() => flatListRef.current?.scrollToEnd({ animated: true })}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text variant="bodyMedium" style={styles.emptyText}>
              No messages yet. Start the conversation!
            </Text>
          </View>
        }
      />

      <Card style={styles.infoCard}>
        <Card.Content>
          <View style={styles.infoRow}>
            <Text variant="bodySmall" style={styles.infoText}>
              {isConnected ? '🟢 Connected' : '🟡 Polling'}
            </Text>
            <Text variant="bodySmall" style={styles.infoText}>
              {getExpiryCountdown()}
            </Text>
          </View>
        </Card.Content>
      </Card>

      <View style={styles.inputContainer}>
        <TextInput
          mode="outlined"
          placeholder="Type a message..."
          value={messageText}
          onChangeText={setMessageText}
          style={styles.input}
          multiline
          maxLength={500}
        />
        <Button
          mode="contained"
          onPress={handleSend}
          loading={sending}
          disabled={sending || !messageText.trim()}
          style={styles.sendButton}
        >
          Send
        </Button>
      </View>

      <Button
        mode="outlined"
        onPress={handleHeadingThere}
        style={styles.headingButton}
        icon="walk"
      >
        Heading There
      </Button>

      <ReportModal
        visible={showReportModal}
        onClose={() => {
          setShowReportModal(false);
          setReportTargetUserId(null);
        }}
        onSubmit={submitReport}
        targetUserName={
          reportTargetUserId === activity?.createdBy
            ? activity?.createdByName
            : `User #${reportTargetUserId}`
        }
      />
    </KeyboardAvoidingView>
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
  headerCard: {
    margin: 8,
    marginBottom: 4,
  },
  activityTitle: {
    fontWeight: '600',
    marginBottom: 8,
  },
  meta: {
    flexDirection: 'row',
    gap: 12,
    flexWrap: 'wrap',
  },
  messagesList: {
    paddingVertical: 8,
    paddingBottom: 120,
  },
  empty: {
    padding: 32,
    alignItems: 'center',
  },
  emptyText: {
    color: '#666',
  },
  infoCard: {
    margin: 8,
    marginTop: 4,
    backgroundColor: '#FFF3E0',
  },
  infoText: {
    color: '#E65100',
    textAlign: 'center',
  },
  inputContainer: {
    flexDirection: 'row',
    padding: 8,
    backgroundColor: '#FFF',
    borderTopWidth: 1,
    borderTopColor: '#E0E0E0',
    alignItems: 'flex-end',
  },
  input: {
    flex: 1,
    marginRight: 8,
    maxHeight: 100,
  },
  sendButton: {
    alignSelf: 'flex-end',
  },
  headingButton: {
    margin: 8,
    marginTop: 0,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  headerContent: {
    flex: 1,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
});

