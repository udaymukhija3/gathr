import React, { useEffect, useState, useRef } from 'react';
import { View, StyleSheet, FlatList, KeyboardAvoidingView, Platform } from 'react-native';
import { TextInput, Button, Text, ActivityIndicator, Card } from 'react-native-paper';
import { format, parseISO, differenceInMinutes } from 'date-fns';
import { Message, ActivityDetail } from '../types';
import { messagesApi, activitiesApi } from '../services/api';
import { ChatBubble } from '../components/ChatBubble';

interface ChatScreenProps {
  activityId: number;
}

export const ChatScreen: React.FC<ChatScreenProps> = ({ activityId }) => {
  const [activity, setActivity] = useState<ActivityDetail | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [messageText, setMessageText] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const flatListRef = useRef<FlatList>(null);
  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    loadActivity();
    loadMessages();
    startPolling();

    return () => {
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
      }
    };
  }, [activityId]);

  const loadActivity = async () => {
    try {
      const data = await activitiesApi.getById(activityId);
      setActivity(data);
    } catch (error) {
      console.error('Error loading activity:', error);
    }
  };

  const loadMessages = async () => {
    try {
      const data = await messagesApi.getByActivity(activityId);
      setMessages(data);
      setLoading(false);
      // Scroll to bottom after loading
      setTimeout(() => {
        flatListRef.current?.scrollToEnd({ animated: false });
      }, 100);
    } catch (error) {
      console.error('Error loading messages:', error);
      setLoading(false);
    }
  };

  const startPolling = () => {
    // Poll for new messages every 3 seconds
    pollIntervalRef.current = setInterval(() => {
      loadMessages();
    }, 3000);
  };

  const handleSend = async () => {
    if (!messageText.trim()) return;

    const text = messageText.trim();
    setMessageText('');
    setSending(true);

    try {
      await messagesApi.create(activityId, text);
      await loadMessages();
    } catch (error) {
      console.error('Error sending message:', error);
      setMessageText(text); // Restore message on error
    } finally {
      setSending(false);
    }
  };

  const handleHeadingThere = async () => {
    try {
      await activitiesApi.join(activityId, 'CONFIRMED');
      await loadActivity();
      // Send a system message
      await messagesApi.create(activityId, 'Heading there now! 🚶‍♂️');
      await loadMessages();
    } catch (error) {
      console.error('Error confirming:', error);
    }
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
          </Card.Content>
        </Card>
      )}

      <FlatList
        ref={flatListRef}
        data={messages}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => (
          <ChatBubble message={item} isOwn={item.userId === 1} />
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
          <Text variant="bodySmall" style={styles.infoText}>
            💬 Messages auto-clear 24 hours after activity ends
          </Text>
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
});

