import React, { useState, useEffect } from 'react';
import { StyleSheet, KeyboardAvoidingView, Platform } from 'react-native';
import { TextInput, Button, Text, Surface } from 'react-native-paper';
import Toast from 'react-native-toast-message';
import { useApi } from '../hooks/useApi';
import { trackAuth } from '../utils/telemetry';

interface PhoneEntryScreenProps {
  onOtpSent: (phone: string) => void;
  navigation?: any;
}

export const PhoneEntryScreen: React.FC<PhoneEntryScreenProps> = ({ onOtpSent, navigation }) => {
  const [phone, setPhone] = useState('');
  const { request, loading } = useApi();
  const [error, setError] = useState('');

  useEffect(() => {
    trackAuth.phoneEntryViewed();
  }, []);

  const handleSubmit = async () => {
    if (!phone || phone.length < 10) {
      setError('Please enter a valid phone number');
      return;
    }

    setError('');

    try {
      await request({
        method: 'POST',
        url: '/auth/otp/start',
        data: { phone },
      });

      trackAuth.otpRequested(phone);

      Toast.show({
        type: 'success',
        text1: 'OTP Sent',
        text2: 'Check your phone for the verification code',
      });

      onOtpSent(phone);
    } catch (err: any) {
      const errorMessage = err.message || 'Failed to send OTP';
      setError(errorMessage);

      trackAuth.otpRequestFailed(phone, errorMessage);

      if (errorMessage.includes('429') || errorMessage.includes('Too many requests')) {
        Toast.show({
          type: 'error',
          text1: 'Rate Limit Exceeded',
          text2: 'Please wait before requesting another OTP',
        });
      } else {
        Toast.show({
          type: 'error',
          text1: 'Error',
          text2: errorMessage,
        });
      }
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={styles.container}
    >
      <Surface style={styles.surface}>
        <Text variant="headlineMedium" style={styles.title}>
          Welcome to gathr
        </Text>
        <Text variant="bodyMedium" style={styles.subtitle}>
          Enter your phone number to get started
        </Text>

        <TextInput
          label="Phone Number"
          value={phone}
          onChangeText={setPhone}
          keyboardType="phone-pad"
          mode="outlined"
          style={styles.input}
          error={!!error}
          maxLength={10}
          left={<TextInput.Icon icon="phone" />}
        />

        {error && (
          <Text variant="bodySmall" style={styles.error}>
            {error}
          </Text>
        )}

        <Button
          mode="contained"
          onPress={handleSubmit}
          loading={loading}
          disabled={loading}
          style={styles.button}
        >
          Send OTP
        </Button>
      </Surface>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 16,
    backgroundColor: '#F5F5F5',
  },
  surface: {
    padding: 24,
    borderRadius: 16,
    elevation: 4,
  },
  title: {
    textAlign: 'center',
    marginBottom: 8,
    fontWeight: 'bold',
  },
  subtitle: {
    textAlign: 'center',
    marginBottom: 32,
    color: '#666',
  },
  input: {
    marginBottom: 16,
  },
  error: {
    color: '#B00020',
    marginBottom: 16,
    marginLeft: 16,
  },
  button: {
    marginTop: 8,
  },
});

