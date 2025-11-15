import React from 'react';
import { View, StyleSheet } from 'react-native';
import { Text } from 'react-native-paper';

interface MutualBadgeProps {
  count: number;
}

export const MutualBadge: React.FC<MutualBadgeProps> = ({ count }) => {
  if (count === 0) {
    return null;
  }

  return (
    <View style={styles.badge}>
      <Text variant="bodySmall" style={styles.text}>
        🤝 {count} mutual{count !== 1 ? 's' : ''}
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  badge: {
    backgroundColor: '#E3F2FD',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
    alignSelf: 'flex-start',
  },
  text: {
    color: '#1976D2',
    fontSize: 12,
  },
});

