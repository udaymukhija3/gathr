import React, { useEffect, useState } from 'react';
import { View, StyleSheet, ScrollView, Share, Alert } from 'react-native';
import { TextInput, Button, Text, Card, Surface } from 'react-native-paper';
import * as Clipboard from 'expo-clipboard';
import * as Sharing from 'expo-sharing';
import { ActivityDetail } from '../types';
import { activitiesApi } from '../services/api';

interface InviteScreenProps {
  activityId: number;
}

export const InviteScreen: React.FC<InviteScreenProps> = ({ activityId }) => {
  const [activity, setActivity] = useState<ActivityDetail | null>(null);
  const [phone, setPhone] = useState('');
  const [loading, setLoading] = useState(false);
  const [inviteLink, setInviteLink] = useState('');

  useEffect(() => {
    loadActivity();
    generateInviteLink();
  }, [activityId]);

  const loadActivity = async () => {
    try {
      const data = await activitiesApi.getById(activityId);
      setActivity(data);
    } catch (error) {
      console.error('Error loading activity:', error);
    }
  };

  const generateInviteLink = () => {
    const baseUrl = process.env.EXPO_PUBLIC_APP_URL || 'https://gathr.app';
    const link = `${baseUrl}/join/${activityId}`;
    setInviteLink(link);
  };

  const handleCopyLink = async () => {
    try {
      await Clipboard.setStringAsync(inviteLink);
      Alert.alert('Success', 'Invite link copied to clipboard!');
    } catch (error) {
      Alert.alert('Error', 'Failed to copy link');
    }
  };

  const handleShare = async () => {
    try {
      const isAvailable = await Sharing.isAvailableAsync();
      if (isAvailable) {
        await Sharing.shareAsync(inviteLink, {
          message: `Join me for ${activity?.title || 'this activity'} on gathr! ${inviteLink}`,
        });
      } else {
        await handleCopyLink();
      }
    } catch (error) {
      console.error('Error sharing:', error);
    }
  };

  const handleInviteByPhone = async () => {
    if (!phone || phone.length !== 10) {
      Alert.alert('Error', 'Please enter a valid 10-digit phone number');
      return;
    }

    setLoading(true);
    try {
      await activitiesApi.invite(activityId, phone);
      Alert.alert('Success', `Invitation sent to ${phone}`);
      setPhone('');
    } catch (error) {
      Alert.alert('Error', 'Failed to send invitation');
    } finally {
      setLoading(false);
    }
  };

  if (!activity) {
    return (
      <View style={styles.center}>
        <Text>Loading...</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      <Card style={styles.card}>
        <Card.Content>
          <Text variant="headlineSmall" style={styles.title}>
            Invite Friends
          </Text>
          <Text variant="bodyMedium" style={styles.subtitle}>
            {activity.title}
          </Text>
        </Card.Content>
      </Card>

      <Card style={styles.card}>
        <Card.Content>
          <Text variant="titleMedium" style={styles.sectionTitle}>
            Share Invite Link
          </Text>
          <Surface style={styles.linkContainer}>
            <Text variant="bodySmall" style={styles.link} numberOfLines={2}>
              {inviteLink}
            </Text>
          </Surface>
          <View style={styles.buttonRow}>
            <Button mode="outlined" onPress={handleCopyLink} style={styles.button}>
              Copy Link
            </Button>
            <Button mode="contained" onPress={handleShare} style={styles.button}>
              Share
            </Button>
          </View>
        </Card.Content>
      </Card>

      <Card style={styles.card}>
        <Card.Content>
          <Text variant="titleMedium" style={styles.sectionTitle}>
            Invite by Phone
          </Text>
          <TextInput
            label="Phone Number"
            value={phone}
            onChangeText={setPhone}
            keyboardType="phone-pad"
            mode="outlined"
            style={styles.input}
            maxLength={10}
            left={<TextInput.Icon icon="phone" />}
          />
          <Button
            mode="contained"
            onPress={handleInviteByPhone}
            loading={loading}
            disabled={loading}
            style={styles.button}
          >
            Send Invitation
          </Button>
          <Text variant="bodySmall" style={styles.note}>
            📱 An SMS invitation will be sent (mock in development)
          </Text>
        </Card.Content>
      </Card>

      <Card style={styles.card}>
        <Card.Content>
          <Text variant="titleMedium" style={styles.sectionTitle}>
            Bring a +1
          </Text>
          <Text variant="bodyMedium" style={styles.description}>
            Share the invite link with friends who might be interested. The more, the merrier! 🎉
          </Text>
          {activity.isInviteOnly && (
            <Text variant="bodySmall" style={styles.warning}>
              ⚠️ This is an invite-only activity. Only invited members can join.
            </Text>
          )}
        </Card.Content>
      </Card>
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
  title: {
    fontWeight: 'bold',
    marginBottom: 4,
  },
  subtitle: {
    color: '#666',
  },
  sectionTitle: {
    marginBottom: 16,
    fontWeight: '600',
  },
  linkContainer: {
    padding: 12,
    borderRadius: 8,
    marginBottom: 16,
    backgroundColor: '#F5F5F5',
  },
  link: {
    color: '#6200EE',
    fontFamily: 'monospace',
  },
  buttonRow: {
    flexDirection: 'row',
    gap: 8,
  },
  button: {
    flex: 1,
    marginTop: 8,
  },
  input: {
    marginBottom: 16,
  },
  note: {
    marginTop: 8,
    color: '#666',
    fontStyle: 'italic',
  },
  description: {
    color: '#666',
    marginBottom: 8,
  },
  warning: {
    marginTop: 8,
    color: '#FF9800',
  },
});

