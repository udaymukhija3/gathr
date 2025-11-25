import React, { useState, useEffect, useCallback } from 'react';
import { StyleSheet, KeyboardAvoidingView, Platform, View } from 'react-native';
import { TextInput, Button, Text, Surface } from 'react-native-paper';
import Toast from 'react-native-toast-message';
import { useUser } from '../context/UserContext';
import { useApi } from '../hooks/useApi';
import { AuthResponse } from '../types';
import { trackAuth } from '../utils/telemetry';

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
  const [attempts, setAttempts] = useState(0);
  const [lockedUntil, setLockedUntil] = useState<Date | null>(null);
  const [countdown, setCountdown] = useState(0);

  const MAX_ATTEMPTS = 5;
  const LOCKOUT_SECONDS = 30;

  // Countdown timer for lockout
  useEffect(() => {
    if (!lockedUntil) return;

    const interval = setInterval(() => {
      const remaining = Math.max(0, Math.ceil((lockedUntil.getTime() - Date.now()) / 1000));
      setCountdown(remaining);

      if (remaining === 0) {
        setLockedUntil(null);
        setAttempts(0);
        setError('');
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [lockedUntil]);

  const handleVerify = async () => {
    if (!otp || otp.length < 4) {
      setError('Please enter a valid OTP');
      return;
    }

    if (lockedUntil && Date.now() < lockedUntil.getTime()) {
      return; // Still locked out
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
      const newAttempts = attempts + 1;
      setAttempts(newAttempts);

      // Handle specific error types
      if (errorMessage.toLowerCase().includes('too many') || errorMessage.includes('429')) {
        // Rate limited by server
        const lockoutEnd = new Date(Date.now() + 60 * 1000);
        setLockedUntil(lockoutEnd);
        setCountdown(60);
        setError('Too many attempts. Please wait before trying again.');
        Toast.show({
          type: 'error',
          text1: 'Too Many Attempts',
          text2: 'Please wait 1 minute before trying again',
        });
      } else if (errorMessage.toLowerCase().includes('invalid') || errorMessage.toLowerCase().includes('expired')) {
        // Wrong OTP or expired
        if (newAttempts >= MAX_ATTEMPTS) {
          const lockoutEnd = new Date(Date.now() + LOCKOUT_SECONDS * 1000);
          setLockedUntil(lockoutEnd);
          setCountdown(LOCKOUT_SECONDS);
          setError(`Too many failed attempts. Please wait ${LOCKOUT_SECONDS} seconds.`);
          Toast.show({
            type: 'error',
            text1: 'Too Many Failed Attempts',
            text2: `Please wait ${LOCKOUT_SECONDS} seconds`,
          });
        } else {
          const remainingAttempts = MAX_ATTEMPTS - newAttempts;
          setError(`Invalid or expired OTP. ${remainingAttempts} attempt${remainingAttempts !== 1 ? 's' : ''} remaining.`);
          Toast.show({
            type: 'error',
            text1: 'Invalid OTP',
            text2: `${remainingAttempts} attempt${remainingAttempts !== 1 ? 's' : ''} remaining`,
          });
        }
      } else if (errorMessage.toLowerCase().includes('network')) {
        setError('Network error. Please check your connection.');
        Toast.show({
          type: 'error',
          text1: 'Connection Error',
          text2: 'Please check your internet connection',
        });
      } else if (errorMessage.toLowerCase().includes('server') || errorMessage.includes('500')) {
        setError('Server error. Please try again later.');
        Toast.show({
          type: 'error',
          text1: 'Server Error',
          text2: 'Please try again in a few moments',
        });
      } else {
        setError(errorMessage);
        Toast.show({
          type: 'error',
          text1: 'Verification Failed',
          text2: errorMessage,
        });
      }
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
              disabled={loading || (lockedUntil !== null && countdown > 0)}
              style={styles.button}
            >
              {countdown > 0 ? `Try again in ${countdown}s` : 'Verify'}
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

