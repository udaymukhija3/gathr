import React from 'react';
import { Avatar, Text } from 'react-native-paper';
import { StyleSheet, View } from 'react-native';

interface AvatarAnonProps {
  name: string;
  revealed?: boolean;
  size?: number;
}

export const AvatarAnon: React.FC<AvatarAnonProps> = ({
  name,
  revealed = false,
  size = 40,
}) => {
  const displayName = revealed ? name.split(' ')[0] : name;
  const initials = displayName
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);

  return (
    <View style={styles.container}>
      <Avatar.Text
        size={size}
        label={initials}
        style={[styles.avatar, !revealed && styles.anonymous]}
      />
      <Text variant="bodySmall" style={styles.name} numberOfLines={1}>
        {displayName}
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    margin: 8,
    width: 60,
  },
  avatar: {
    backgroundColor: '#6200EE',
  },
  anonymous: {
    backgroundColor: '#9E9E9E',
  },
  name: {
    marginTop: 4,
    textAlign: 'center',
    fontSize: 10,
  },
});

