import React, { useState } from 'react';
import { View, StyleSheet, ScrollView, Alert } from 'react-native';
import { Text, Button, List, Switch, Divider } from 'react-native-paper';
import Toast from 'react-native-toast-message';
import { useUser } from '../context/UserContext';
import { useApi } from '../hooks/useApi';

interface SettingsScreenProps {
  navigation?: any;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({ navigation }) => {
  const { user, logout } = useUser();
  const { request } = useApi();
  const [contactsEnabled, setContactsEnabled] = useState(true);

  const handleLogout = async () => {
    Alert.alert(
      'Logout',
      'Are you sure you want to logout?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Logout',
          style: 'destructive',
          onPress: async () => {
            await logout();
            navigation?.reset({
              index: 0,
              routes: [{ name: 'PhoneEntry' }],
            });
          },
        },
      ]
    );
  };

  const handleDeleteAccount = () => {
    Alert.alert(
      'Delete Account',
      'This action cannot be undone. All your data will be permanently deleted.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              await request({
                method: 'DELETE',
                url: '/users/me', // Placeholder endpoint
              });
              Toast.show({
                type: 'success',
                text1: 'Account Deleted',
              });
              await logout();
              navigation?.reset({
                index: 0,
                routes: [{ name: 'PhoneEntry' }],
              });
            } catch (error: any) {
              Toast.show({
                type: 'error',
                text1: 'Error',
                text2: error.message || 'Could not delete account',
              });
            }
          },
        },
      ]
    );
  };

  const handleRevokeContacts = async () => {
    try {
      await request({
        method: 'DELETE',
        url: '/contacts/upload', // Clear contacts
      });
      setContactsEnabled(false);
      Toast.show({
        type: 'success',
        text1: 'Contacts Revoked',
        text2: 'Your contacts have been removed',
      });
    } catch (error: any) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: error.message || 'Could not revoke contacts',
      });
    }
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.section}>
        <Text variant="titleMedium" style={styles.sectionTitle}>
          Account
        </Text>
        <List.Item
          title={user?.name || user?.phone || 'User'}
          description={user?.phone}
          left={(props) => <List.Icon {...props} icon="account" />}
        />
      </View>

      <Divider />

      <View style={styles.section}>
        <Text variant="titleMedium" style={styles.sectionTitle}>
          Privacy
        </Text>
        <List.Item
          title="Contact Upload"
          description="Share hashed contacts for mutual discovery"
          left={(props) => <List.Icon {...props} icon="contacts" />}
          right={() => (
            <Switch
              value={contactsEnabled}
              onValueChange={(value) => {
                if (value) {
                  // Navigate to contacts upload screen
                  navigation?.navigate('ContactsUpload');
                } else {
                  handleRevokeContacts();
                }
              }}
            />
          )}
        />
        <List.Item
          title="Privacy Policy"
          description="Read our privacy policy"
          left={(props) => <List.Icon {...props} icon="file-document" />}
          onPress={() => {
            // Open privacy policy
            Toast.show({
              type: 'info',
              text1: 'Privacy Policy',
              text2: 'Open in browser or app',
            });
          }}
        />
      </View>

      <Divider />

      <View style={styles.section}>
        <Button
          mode="outlined"
          onPress={handleLogout}
          style={styles.button}
          textColor="#B00020"
        >
          Logout
        </Button>

        <Button
          mode="outlined"
          onPress={handleDeleteAccount}
          style={[styles.button, styles.deleteButton]}
          textColor="#B00020"
        >
          Delete Account
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
  section: {
    backgroundColor: '#FFF',
    paddingVertical: 8,
  },
  sectionTitle: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    fontWeight: '600',
    color: '#666',
  },
  button: {
    marginHorizontal: 16,
    marginVertical: 8,
  },
  deleteButton: {
    borderColor: '#B00020',
  },
});

