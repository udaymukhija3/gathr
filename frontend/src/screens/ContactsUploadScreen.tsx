import React, { useState, useEffect } from 'react';
import { View, StyleSheet, ScrollView } from 'react-native';
import { Text, Button, Surface, Checkbox, Card } from 'react-native-paper';
import Toast from 'react-native-toast-message';
import { useApi } from '../hooks/useApi';
import { getHashedContacts } from '../utils/contactHashing';
import { ContactUploadResponse } from '../types';
import { trackContacts } from '../utils/telemetry';

interface ContactsUploadScreenProps {
  onComplete?: () => void;
  onSkip?: () => void;
}

export const ContactsUploadScreen: React.FC<ContactsUploadScreenProps> = ({
  onComplete,
  onSkip,
}) => {
  const [consentGiven, setConsentGiven] = useState(false);
  const [uploading, setUploading] = useState(false);
  const { request } = useApi();

  useEffect(() => {
    // Track screen viewed
    trackContacts.screenViewed();
  }, []);

  const handleUpload = async () => {
    if (!consentGiven) {
      Toast.show({
        type: 'error',
        text1: 'Consent Required',
        text2: 'Please read and accept the privacy notice',
      });
      return;
    }

    setUploading(true);

    try {
      const hashes = await getHashedContacts();
      trackContacts.uploadStarted(hashes.length);

      const response = await request<ContactUploadResponse>({
        method: 'POST',
        url: '/contacts/upload',
        data: { hashes },
      });

      trackContacts.uploadSuccess(response.mutualsCount, {
        totalContacts: hashes.length,
      });
      Toast.show({
        type: 'success',
        text1: 'Contacts Uploaded',
        text2: `Found ${response.mutualsCount} mutual contacts`,
      });

      onComplete?.();
    } catch (error: any) {
      if (error.message?.includes('permission')) {
        trackContacts.permissionDenied();
        Toast.show({
          type: 'error',
          text1: 'Permission Denied',
          text2: 'Please grant contacts permission in settings',
        });
      } else {
        trackContacts.uploadFailed(error.message || 'Unknown error');
        Toast.show({
          type: 'error',
          text1: 'Upload Failed',
          text2: error.message || 'Could not upload contacts',
        });
      }
    } finally {
      setUploading(false);
    }
  };

  return (
    <ScrollView style={styles.container}>
      <Surface style={styles.surface}>
        <Text variant="headlineSmall" style={styles.title}>
          Find Mutual Contacts
        </Text>

        <Card style={styles.infoCard}>
          <Card.Content>
            <Text variant="bodyMedium" style={styles.infoText}>
              Share hashed contacts to surface mutuals in activities.
            </Text>
            <Text variant="bodySmall" style={styles.infoDetail}>
              • We only upload hashed phone numbers{'\n'}
              • We never reveal names or contact details{'\n'}
              • Hashes are computed on your device{'\n'}
              • You can revoke this anytime in settings
            </Text>
          </Card.Content>
        </Card>

        <View style={styles.checkboxContainer}>
          <Checkbox
            status={consentGiven ? 'checked' : 'unchecked'}
            onPress={() => {
              const newValue = !consentGiven;
              trackContacts.consentToggled(newValue);
              setConsentGiven(newValue);
            }}
          />
          <Text variant="bodyMedium" style={styles.checkboxLabel}>
            I understand and consent to sharing hashed contacts for mutual discovery
          </Text>
        </View>

        <Button
          mode="contained"
          onPress={handleUpload}
          loading={uploading}
          disabled={uploading || !consentGiven}
          style={styles.button}
        >
          Upload Contacts
        </Button>

        {onSkip && (
          <Button
            mode="text"
            onPress={() => {
              trackContacts.skipped();
              onSkip();
            }}
            style={styles.skipButton}
          >
            Skip for Now
          </Button>
        )}
      </Surface>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  surface: {
    margin: 16,
    padding: 24,
    borderRadius: 16,
    elevation: 4,
  },
  title: {
    fontWeight: 'bold',
    marginBottom: 24,
    textAlign: 'center',
  },
  infoCard: {
    marginBottom: 24,
    backgroundColor: '#E3F2FD',
  },
  infoText: {
    marginBottom: 12,
    fontWeight: '600',
  },
  infoDetail: {
    color: '#666',
    lineHeight: 20,
  },
  checkboxContainer: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginBottom: 24,
  },
  checkboxLabel: {
    flex: 1,
    marginLeft: 8,
    lineHeight: 24,
  },
  button: {
    marginTop: 8,
  },
  skipButton: {
    marginTop: 8,
  },
});

