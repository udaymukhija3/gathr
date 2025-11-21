import React, { useState } from 'react';
import { StyleSheet, KeyboardAvoidingView, Platform } from 'react-native';
import { TextInput, Button, Text, Surface } from 'react-native-paper';
import Toast from 'react-native-toast-message';
import { useUser } from '../context/UserContext';
import { useApi } from '../hooks/useApi';
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
  const [displayName, setDisplayName] = useState('');
  const [showNameInput, setShowNameInput] = useState(false);
  const { login } = useUser();
  const { request, loading } = useApi();
  const [error, setError] = useState('');

  const handleVerify = async () => {
    if (!otp || otp.length < 4) {
      setError('Please enter a valid OTP');
      return;
    }

    setError('');

    try {
      const response = await request<AuthResponse>({
        method: 'POST',
        url: '/auth/otp/verify',
        data: { phone, otp },
      });

      await login(response);
      
      Toast.show({
        type: 'success',
        text1: 'Welcome!',
        text2: 'You\'re all set',
      });

      // Optional: Prompt for display name
      if (!response.user.name || response.user.name === phone) {
        setShowNameInput(true);
      } else {
        onVerified(response);
      }
    } catch (err: any) {
      const errorMessage = err.message || 'Invalid OTP';
      setError(errorMessage);
      Toast.show({
        type: 'error',
        text1: 'Verification Failed',
        text2: errorMessage,
      });
    }
  };

  const handleNameSubmit = () => {
    // In production, update user name via API
    onVerified({ token: '', user: { id: 0, name: displayName, phone, verified: true, createdAt: '' } });
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

        {!showNameInput ? (
          <>
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
          </>
        ) : (
          <>
            <Text variant="bodyMedium" style={styles.subtitle}>
              What should we call you?
            </Text>
            <TextInput
              label="Display Name"
              value={displayName}
              onChangeText={setDisplayName}
              mode="outlined"
              style={styles.input}
              left={<TextInput.Icon icon="account" />}
            />
            <Button
              mode="contained"
              onPress={handleNameSubmit}
              disabled={!displayName.trim()}
              style={styles.button}
            >
              Continue
            </Button>
          </>
        )}
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

