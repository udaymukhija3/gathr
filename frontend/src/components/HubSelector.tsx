import React from 'react';
import { View, StyleSheet, ScrollView } from 'react-native';
import { Chip } from 'react-native-paper';
import { Hub } from '../types';

interface HubSelectorProps {
  hubs: Hub[];
  selectedHubId: number | null;
  onSelectHub: (hubId: number) => void;
}

export const HubSelector: React.FC<HubSelectorProps> = ({
  hubs,
  selectedHubId,
  onSelectHub,
}) => {
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={styles.container}
    >
      {hubs.map((hub) => (
        <Chip
          key={hub.id}
          selected={selectedHubId === hub.id}
          onPress={() => onSelectHub(hub.id)}
          style={styles.chip}
          mode={selectedHubId === hub.id ? 'flat' : 'outlined'}
        >
          {hub.name}
        </Chip>
      ))}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 8,
  },
  chip: {
    marginRight: 8,
  },
});

