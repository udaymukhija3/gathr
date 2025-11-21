import React, { useEffect, useState } from 'react';
import { View, StyleSheet, ScrollView, FlatList } from 'react-native';
import { Text, Card, Button, Chip, ActivityIndicator, IconButton } from 'react-native-paper';
import Toast from 'react-native-toast-message';
import { ActivityTemplate } from '../types';
import { templatesApi } from '../services/api';

interface TemplateSelectionScreenProps {
  onSelectTemplate: (template: ActivityTemplate | null) => void;
  onCancel: () => void;
}

export const TemplateSelectionScreen: React.FC<TemplateSelectionScreenProps> = ({
  onSelectTemplate,
  onCancel,
}) => {
  const [templates, setTemplates] = useState<ActivityTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'system' | 'user'>('all');

  useEffect(() => {
    loadTemplates();
  }, [filter]);

  const loadTemplates = async () => {
    try {
      setLoading(true);
      const data = await templatesApi.getAll(filter);
      setTemplates(data);
    } catch (error) {
      console.error('Error loading templates:', error);
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Failed to load templates',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteTemplate = async (templateId: number) => {
    try {
      await templatesApi.delete(templateId);
      Toast.show({
        type: 'success',
        text1: 'Success',
        text2: 'Template deleted',
      });
      loadTemplates();
    } catch (error) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Failed to delete template',
      });
    }
  };

  const categoryIcon = (category: string) => {
    switch (category) {
      case 'FOOD':
        return '🍔';
      case 'SPORTS':
        return '⚽';
      case 'ART':
        return '🎨';
      case 'MUSIC':
        return '🎵';
      default:
        return '📌';
    }
  };

  const renderTemplate = ({ item }: { item: ActivityTemplate }) => (
    <Card style={styles.templateCard}>
      <Card.Content>
        <View style={styles.templateHeader}>
          <View style={styles.templateInfo}>
            <Text variant="titleMedium" style={styles.templateName}>
              {categoryIcon(item.category)} {item.name}
            </Text>
            <Text variant="bodySmall" style={styles.templateTitle}>
              {item.title}
            </Text>
          </View>
          {!item.isSystemTemplate && (
            <IconButton
              icon="delete"
              size={20}
              onPress={() => handleDeleteTemplate(item.id)}
            />
          )}
        </View>

        {item.description && (
          <Text variant="bodySmall" style={styles.description}>
            {item.description}
          </Text>
        )}

        <View style={styles.templateMeta}>
          <Chip style={styles.chip} compact>
            {item.category}
          </Chip>
          {item.durationHours && (
            <Chip style={styles.chip} icon="clock" compact>
              {item.durationHours}h
            </Chip>
          )}
          <Chip style={styles.chip} icon="account-group" compact>
            Max {item.maxMembers}
          </Chip>
          {item.isInviteOnly && (
            <Chip style={styles.chip} icon="lock" compact>
              Invite Only
            </Chip>
          )}
        </View>
      </Card.Content>
      <Card.Actions>
        <Button onPress={() => onSelectTemplate(item)}>Use Template</Button>
      </Card.Actions>
    </Card>
  );

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Card style={styles.headerCard}>
        <Card.Content>
          <Text variant="headlineSmall" style={styles.title}>
            Choose a Template
          </Text>
          <Text variant="bodyMedium" style={styles.subtitle}>
            Start from a template or create from scratch
          </Text>
        </Card.Content>
      </Card>

      <View style={styles.filterContainer}>
        <Chip
          selected={filter === 'all'}
          onPress={() => setFilter('all')}
          style={styles.filterChip}
        >
          All
        </Chip>
        <Chip
          selected={filter === 'system'}
          onPress={() => setFilter('system')}
          style={styles.filterChip}
        >
          Pre-built
        </Chip>
        <Chip
          selected={filter === 'user'}
          onPress={() => setFilter('user')}
          style={styles.filterChip}
        >
          My Templates
        </Chip>
      </View>

      <FlatList
        data={templates}
        renderItem={renderTemplate}
        keyExtractor={(item) => item.id.toString()}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={
          <Card style={styles.emptyCard}>
            <Card.Content>
              <Text variant="bodyMedium" style={styles.emptyText}>
                No templates found. {filter === 'user' && 'Create an activity and save it as a template!'}
              </Text>
            </Card.Content>
          </Card>
        }
      />

      <View style={styles.actions}>
        <Button mode="outlined" onPress={onCancel} style={styles.button}>
          Cancel
        </Button>
        <Button
          mode="contained"
          onPress={() => onSelectTemplate(null)}
          style={styles.button}
        >
          Start from Scratch
        </Button>
      </View>
    </View>
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
    margin: 16,
    marginBottom: 8,
  },
  title: {
    fontWeight: 'bold',
  },
  subtitle: {
    marginTop: 4,
    color: '#666',
  },
  filterContainer: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingBottom: 8,
    gap: 8,
  },
  filterChip: {
    marginRight: 4,
  },
  listContent: {
    padding: 16,
    paddingTop: 8,
  },
  templateCard: {
    marginBottom: 16,
  },
  templateHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  templateInfo: {
    flex: 1,
  },
  templateName: {
    fontWeight: '600',
    marginBottom: 4,
  },
  templateTitle: {
    color: '#666',
  },
  description: {
    marginBottom: 12,
    color: '#666',
  },
  templateMeta: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  chip: {
    marginRight: 4,
    marginBottom: 4,
  },
  emptyCard: {
    marginTop: 32,
  },
  emptyText: {
    textAlign: 'center',
    color: '#666',
  },
  actions: {
    flexDirection: 'row',
    padding: 16,
    gap: 8,
    backgroundColor: '#FFF',
    borderTopWidth: 1,
    borderTopColor: '#E0E0E0',
  },
  button: {
    flex: 1,
  },
});
