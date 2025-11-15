import React, { useState } from 'react';
import { View, StyleSheet, KeyboardAvoidingView, Platform } from 'react-native';
import { TextInput, Button, Text, Surface } from 'react-native-paper';
import { authApi } from '../services/api';
import { AuthResponse } from '../types';

interface OtpVerifyScreenProps {
  phone: string;
  onVerified: (authResponse: AuthResponse) => void;
  onBack: () => void;
}

export const OtpVerifyScreen: React.FC<OtpVerifyScreenProps> = ({
  phone,
  onVerified,
  onBack,
}) => {
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleVerify = async () => {
    if (!otp || otp.length < 4) {
      setError('Please enter a valid OTP');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const response = await authApi.verifyOtp(phone, otp);
      onVerified(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid OTP');
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={styles.container}
    >
      <Surface style={styles.surface}>
        <Text variant="headlineMedium" style={styles.title}>
          Verify OTP
        </Text>
        <Text variant="bodyMedium" style={styles.subtitle}>
          Enter the OTP sent to {phone}
        </Text>

        <TextInput
          label="OTP"
          value={otp}
          onChangeText={setOtp}
          keyboardType="number-pad"
          mode="outlined"
          style={styles.input}
          error={!!error}
          maxLength={6}
          left={<TextInput.Icon icon="key" />}
        />

        {error && (
          <Text variant="bodySmall" style={styles.error}>
            {error}
          </Text>
        )}

        <Button
          mode="contained"
          onPress={handleVerify}
          loading={loading}
          disabled={loading}
          style={styles.button}
        >
          Verify
        </Button>

        <Button mode="text" onPress={onBack} style={styles.backButton}>
          Change Phone Number
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
  backButton: {
    marginTop: 8,
  },
});

