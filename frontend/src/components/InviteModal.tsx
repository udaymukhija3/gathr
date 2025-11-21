import React, { useState } from 'react';
import { View, StyleSheet, Modal } from 'react-native';
import { TextInput, Button, Text, Surface, Portal } from 'react-native-paper';
import * as Clipboard from 'expo-clipboard';
import Toast from 'react-native-toast-message';
import { useApi } from '../hooks/useApi';
import { trackActivity } from '../utils/telemetry';

interface InviteModalProps {
  visible: boolean;
  activityId: number;
  onClose: () => void;
  onTokenEntered?: (token: string) => void;
}

export const InviteModal: React.FC<InviteModalProps> = ({
  visible,
  activityId,
  onClose,
  onTokenEntered,
}) => {
  const [inviteToken, setInviteToken] = useState('');
  const [phone, setPhone] = useState('');
  const [mode, setMode] = useState<'token' | 'sms'>('token');
  const { request, loading } = useApi();

  const handleTokenSubmit = () => {
    if (!inviteToken.trim()) {
      Toast.show({
        type: 'error',
        text1: 'Invalid Token',
        text2: 'Please enter an invite token',
      });
      trackActivity.inviteTokenSubmitted(activityId, false, { reason: 'empty_token' });
      return;
    }
    trackActivity.inviteTokenSubmitted(activityId, true);
    onTokenEntered?.(inviteToken);
    onClose();
  };

  const handleSendSMS = async () => {
    if (!phone.trim()) {
      Toast.show({
        type: 'error',
        text1: 'Invalid Phone',
        text2: 'Please enter a phone number',
      });
      return;
    }

    try {
      await request({
        method: 'POST',
        url: `/activities/${activityId}/invite`,
        data: { phone },
      });

      trackActivity.inviteSMSSent(activityId, { phoneProvided: true });
      Toast.show({
        type: 'success',
        text1: 'Invite Sent',
        text2: 'Invitation sent via SMS',
      });
      onClose();
    } catch (error: any) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: error.message || 'Could not send invite',
      });
    }
  };

  const handleCopyLink = async () => {
    const link = `gathr://activity/${activityId}?token=${inviteToken}`;
    await Clipboard.setStringAsync(link);
    trackActivity.inviteLinkCopied(activityId);
    Toast.show({
      type: 'success',
      text1: 'Link Copied',
      text2: 'Share this link with friends',
    });
  };

  const handleGenerateToken = async () => {
    try {
      const response = await request<{ token: string; expiresAt: string }>({
        method: 'POST',
        url: `/activities/${activityId}/invite-token`,
      });

      setInviteToken(response.token);
      trackActivity.inviteTokenGenerated(activityId);
      Toast.show({
        type: 'success',
        text1: 'Token Generated',
        text2: 'Token copied to clipboard',
      });
      await Clipboard.setStringAsync(response.token);
    } catch (error: any) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: error.message || 'Could not generate token',
      });
    }
  };

  return (
    <Portal>
      <Modal
        visible={visible}
        transparent
        animationType="slide"
        onRequestClose={onClose}
      >
        <View style={styles.overlay}>
          <Surface style={styles.modal}>
            <Text variant="headlineSmall" style={styles.title}>
              Invite Friends
            </Text>

            <View style={styles.tabs}>
              <Button
                mode={mode === 'token' ? 'contained' : 'outlined'}
                onPress={() => setMode('token')}
                style={styles.tabButton}
              >
                Enter Token
              </Button>
              <Button
                mode={mode === 'sms' ? 'contained' : 'outlined'}
                onPress={() => setMode('sms')}
                style={styles.tabButton}
              >
                Send SMS
              </Button>
            </View>

            {mode === 'token' ? (
              <>
                <TextInput
                  label="Invite Token"
                  value={inviteToken}
                  onChangeText={setInviteToken}
                  mode="outlined"
                  style={styles.input}
                  placeholder="Enter invite token"
                />
                <Button
                  mode="text"
                  onPress={handleGenerateToken}
                  style={styles.generateButton}
                >
                  Generate New Token
                </Button>
                <Button
                  mode="contained"
                  onPress={handleTokenSubmit}
                  loading={loading}
                  style={styles.button}
                >
                  Join with Token
                </Button>
                {inviteToken && (
                  <Button
                    mode="outlined"
                    onPress={handleCopyLink}
                    style={styles.button}
                    icon="content-copy"
                  >
                    Copy Invite Link
                  </Button>
                )}
              </>
            ) : (
              <>
                <TextInput
                  label="Phone Number"
                  value={phone}
                  onChangeText={setPhone}
                  keyboardType="phone-pad"
                  mode="outlined"
                  style={styles.input}
                  placeholder="Enter phone number"
                />
                <Button
                  mode="contained"
                  onPress={handleSendSMS}
                  loading={loading}
                  style={styles.button}
                >
                  Send Invite
                </Button>
              </>
            )}

            <Button mode="text" onPress={onClose} style={styles.cancelButton}>
              Cancel
            </Button>
          </Surface>
        </View>
      </Modal>
    </Portal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  modal: {
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 24,
    maxHeight: '80%',
  },
  title: {
    fontWeight: 'bold',
    marginBottom: 24,
    textAlign: 'center',
  },
  tabs: {
    flexDirection: 'row',
    marginBottom: 24,
    gap: 8,
  },
  tabButton: {
    flex: 1,
  },
  input: {
    marginBottom: 16,
  },
  generateButton: {
    marginBottom: 8,
  },
  button: {
    marginTop: 8,
  },
  cancelButton: {
    marginTop: 16,
  },
});

