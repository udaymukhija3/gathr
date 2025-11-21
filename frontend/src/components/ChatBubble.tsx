import React from 'react';
import { View, StyleSheet, TouchableOpacity } from 'react-native';
import { Text, IconButton } from 'react-native-paper';
import { format, parseISO } from 'date-fns';
import { Message } from '../types';

interface ChatBubbleProps {
  message: Message;
  isOwn: boolean;
  onReport?: () => void;
}

export const ChatBubble: React.FC<ChatBubbleProps> = ({ message, isOwn, onReport }) => {
  const time = parseISO(message.createdAt);

  return (
    <View style={[styles.container, isOwn && styles.ownContainer]}>
      {!isOwn && (
        <View style={styles.userNameRow}>
          <Text variant="bodySmall" style={styles.userName}>
            {message.userName}
          </Text>
          {onReport && (
            <IconButton
              icon="flag-outline"
              size={14}
              iconColor="#999"
              onPress={onReport}
              style={styles.reportButton}
            />
          )}
        </View>
      )}
      <View style={[styles.bubble, isOwn && styles.ownBubble]}>
        <Text variant="bodyMedium" style={[styles.text, isOwn && styles.ownText]}>
          {message.text}
        </Text>
        <Text variant="bodySmall" style={[styles.time, isOwn && styles.ownTime]}>
          {format(time, 'h:mm a')}
        </Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginVertical: 4,
    marginHorizontal: 16,
    alignItems: 'flex-start',
  },
  ownContainer: {
    alignItems: 'flex-end',
  },
  userNameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
    marginLeft: 12,
  },
  userName: {
    color: '#666',
    fontSize: 12,
  },
  reportButton: {
    margin: 0,
    padding: 0,
  },
  bubble: {
    maxWidth: '75%',
    backgroundColor: '#F5F5F5',
    padding: 12,
    borderRadius: 16,
    borderBottomLeftRadius: 4,
  },
  ownBubble: {
    backgroundColor: '#6200EE',
    borderBottomLeftRadius: 16,
    borderBottomRightRadius: 4,
  },
  text: {
    color: '#000',
    marginBottom: 4,
  },
  ownText: {
    color: '#FFF',
  },
  time: {
    color: '#666',
    fontSize: 10,
    alignSelf: 'flex-end',
  },
  ownTime: {
    color: '#FFF',
    opacity: 0.8,
  },
});

