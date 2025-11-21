import React, { useEffect, useState } from 'react';
import { View, StyleSheet, ScrollView } from 'react-native';
import { TextInput, Button, Text, Card, SegmentedButtons, Checkbox } from 'react-native-paper';
import { Platform } from 'react-native';
import { format } from 'date-fns';
import Toast from 'react-native-toast-message';
import { Hub, ActivityCategory, CreateActivityRequest, ActivityTemplate } from '../types';
import { activitiesApi, hubsApi } from '../services/api';

interface CreateActivityScreenProps {
  template?: ActivityTemplate;
  onSubmit: (activityId: number) => void;
  onCancel: () => void;
}

export const CreateActivityScreen: React.FC<CreateActivityScreenProps> = ({
  template,
  onSubmit,
  onCancel,
}) => {
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [title, setTitle] = useState(template?.title || '');
  const [category, setCategory] = useState<ActivityCategory>(template?.category || 'FOOD');
  const [hubId, setHubId] = useState<number | null>(null);
  const [startTime, setStartTime] = useState(new Date());
  const [endTime, setEndTime] = useState(
    template?.durationHours
      ? new Date(Date.now() + template.durationHours * 60 * 60 * 1000)
      : new Date(Date.now() + 2 * 60 * 60 * 1000)
  );
  const [description, setDescription] = useState(template?.description || '');
  const [inviteOnly, setInviteOnly] = useState(template?.isInviteOnly || false);
  const [maxMembers, setMaxMembers] = useState(template?.maxMembers || 4);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadHubs();
  }, []);

  const loadHubs = async () => {
    try {
      const hubsData = await hubsApi.getAll();
      setHubs(hubsData);
      if (hubsData.length > 0) {
        setHubId(hubsData[0].id);
      }
    } catch (error) {
      console.error('Error loading hubs:', error);
    }
  };

  const handleSubmit = async () => {
    if (!title.trim()) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Please enter a title',
      });
      return;
    }
    if (!hubId) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Please select a hub',
      });
      return;
    }
    if (endTime <= startTime) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'End time must be after start time',
      });
      return;
    }

    setLoading(true);
    try {
      const request: CreateActivityRequest = {
        title: title.trim(),
        hubId,
        category,
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString(),
        description: description.trim() || undefined,
        isInviteOnly: inviteOnly,
        maxMembers,
      };
      const activity = await activitiesApi.create(request);
      Toast.show({
        type: 'success',
        text1: 'Success',
        text2: 'Activity created!',
      });
      onSubmit(activity.id);
    } catch (error) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Failed to create activity',
      });
    } finally {
      setLoading(false);
    }
  };

  const categoryButtons = [
    { value: 'FOOD', label: 'Food' },
    { value: 'SPORTS', label: 'Sports' },
    { value: 'ART', label: 'Art' },
    { value: 'MUSIC', label: 'Music' },
  ];

  return (
    <ScrollView style={styles.container}>
      <Card style={styles.card}>
        <Card.Content>
          <Text variant="headlineSmall" style={styles.title}>
            Create Your Plan
          </Text>
        </Card.Content>
      </Card>

      <Card style={styles.card}>
        <Card.Content>
          <TextInput
            label="Title"
            value={title}
            onChangeText={setTitle}
            mode="outlined"
            style={styles.input}
            placeholder="e.g., Coffee at Galleria"
          />

          <Text variant="bodyMedium" style={styles.label}>
            Category
          </Text>
          <SegmentedButtons
            value={category}
            onValueChange={(value) => setCategory(value as ActivityCategory)}
            buttons={categoryButtons}
            style={styles.segmentedButtons}
          />

          <Text variant="bodyMedium" style={styles.label}>
            Hub
          </Text>
          <View style={styles.hubButtons}>
            {hubs.map((hub) => (
              <Button
                key={hub.id}
                mode={hubId === hub.id ? 'contained' : 'outlined'}
                onPress={() => setHubId(hub.id)}
                style={styles.hubButton}
              >
                {hub.name}
              </Button>
            ))}
          </View>

          <Text variant="bodyMedium" style={styles.label}>
            Start Time
          </Text>
          <TextInput
            label="Start Time"
            value={format(startTime, 'MMM d, yyyy h:mm a')}
            mode="outlined"
            style={styles.input}
            editable={false}
            right={<TextInput.Icon icon="calendar" onPress={() => {
              // For simplicity, use current time + 1 hour
              const newTime = new Date(startTime.getTime() + 60 * 60 * 1000);
              setStartTime(newTime);
            }} />}
          />

          <Text variant="bodyMedium" style={styles.label}>
            End Time
          </Text>
          <TextInput
            label="End Time"
            value={format(endTime, 'MMM d, yyyy h:mm a')}
            mode="outlined"
            style={styles.input}
            editable={false}
            right={<TextInput.Icon icon="calendar" onPress={() => {
              // For simplicity, use end time + 1 hour
              const newTime = new Date(endTime.getTime() + 60 * 60 * 1000);
              setEndTime(newTime);
            }} />}
          />
          <Text variant="bodySmall" style={styles.note}>
            💡 Note: Full date picker coming soon. For now, times are set relative to current time.
          </Text>

          <TextInput
            label="Description (optional)"
            value={description}
            onChangeText={setDescription}
            mode="outlined"
            style={styles.input}
            multiline
            numberOfLines={4}
            placeholder="Tell people more about this activity..."
          />

          <Text variant="bodyMedium" style={styles.label}>
            Max Members
          </Text>
          <TextInput
            label="Maximum Members"
            value={maxMembers.toString()}
            onChangeText={(text) => {
              const num = parseInt(text) || 4;
              setMaxMembers(Math.max(2, Math.min(20, num)));
            }}
            keyboardType="number-pad"
            mode="outlined"
            style={styles.input}
          />

          <View style={styles.checkboxContainer}>
            <Checkbox
              status={inviteOnly ? 'checked' : 'unchecked'}
              onPress={() => setInviteOnly(!inviteOnly)}
            />
            <Text variant="bodyMedium" style={styles.checkboxLabel}>
              Invite only (locked activity)
            </Text>
          </View>

          {template && (
            <Card style={styles.templateBadge}>
              <Card.Content>
                <Text variant="bodySmall" style={styles.templateText}>
                  📝 Using template: {template.name}
                </Text>
              </Card.Content>
            </Card>
          )}
        </Card.Content>
      </Card>

      <View style={styles.actions}>
        <Button mode="outlined" onPress={onCancel} style={styles.button}>
          Cancel
        </Button>
        <Button
          mode="contained"
          onPress={handleSubmit}
          loading={loading}
          disabled={loading}
          style={styles.button}
        >
          Create Activity
        </Button>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  card: {
    margin: 16,
    marginBottom: 8,
  },
  title: {
    fontWeight: 'bold',
  },
  input: {
    marginBottom: 16,
  },
  label: {
    marginTop: 8,
    marginBottom: 8,
    fontWeight: '600',
  },
  segmentedButtons: {
    marginBottom: 16,
  },
  hubButtons: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 16,
  },
  hubButton: {
    marginRight: 8,
    marginBottom: 8,
  },
  timeButton: {
    marginBottom: 16,
  },
  checkboxContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 8,
  },
  checkboxLabel: {
    marginLeft: 8,
  },
  note: {
    marginTop: -8,
    marginBottom: 16,
    color: '#666',
    fontStyle: 'italic',
    fontSize: 12,
  },
  actions: {
    flexDirection: 'row',
    padding: 16,
    gap: 8,
  },
  button: {
    flex: 1,
  },
  templateBadge: {
    marginTop: 16,
    backgroundColor: '#E3F2FD',
  },
  templateText: {
    color: '#1976D2',
    fontStyle: 'italic',
  },
});

