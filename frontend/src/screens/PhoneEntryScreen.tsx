import React, { useState, useEffect, useCallback } from 'react';
import { StyleSheet, KeyboardAvoidingView, Platform, View } from 'react-native';
import { TextInput, Button, Text, Surface, HelperText } from 'react-native-paper';
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
  const [rateLimitedUntil, setRateLimitedUntil] = useState<Date | null>(null);
  const [countdown, setCountdown] = useState(0);

  useEffect(() => {
    trackAuth.phoneEntryViewed();
  }, []);

  // Countdown timer for rate limiting
  useEffect(() => {
    if (!rateLimitedUntil) return;

    const interval = setInterval(() => {
      const remaining = Math.max(0, Math.ceil((rateLimitedUntil.getTime() - Date.now()) / 1000));
      setCountdown(remaining);

      if (remaining === 0) {
        setRateLimitedUntil(null);
        setError('');
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [rateLimitedUntil]);

  const formatCountdown = useCallback((seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    if (mins > 0) {
      return `${mins}m ${secs}s`;
    }
    return `${secs}s`;
  }, []);

  const handleSubmit = async () => {
    if (!phone || phone.length < 10) {
      setError('Please enter a valid phone number');
      return;
    }

    if (rateLimitedUntil && Date.now() < rateLimitedUntil.getTime()) {
      return; // Still rate limited
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
      trackAuth.otpRequestFailed(phone, errorMessage);

      // Handle specific error types
      if (errorMessage.toLowerCase().includes('too many') || errorMessage.includes('429')) {
        // Rate limited - set 60 second cooldown (server allows 3 per hour, but show shorter UI feedback)
        const cooldownEnd = new Date(Date.now() + 60 * 1000);
        setRateLimitedUntil(cooldownEnd);
        setCountdown(60);
        setError('Too many requests. Please wait before trying again.');
        Toast.show({
          type: 'error',
          text1: 'Rate Limit Exceeded',
          text2: 'You can request another OTP in 1 minute',
        });
      } else if (errorMessage.toLowerCase().includes('invalid phone')) {
        setError('Invalid phone number. Please check and try again.');
        Toast.show({
          type: 'error',
          text1: 'Invalid Phone Number',
          text2: 'Please enter a valid phone number',
        });
      } else if (errorMessage.toLowerCase().includes('service') || errorMessage.toLowerCase().includes('unavailable')) {
        setError('SMS service temporarily unavailable. Please try again later.');
        Toast.show({
          type: 'error',
          text1: 'Service Unavailable',
          text2: 'Please try again in a few minutes',
        });
      } else if (errorMessage.toLowerCase().includes('network')) {
        setError('Network error. Please check your connection.');
        Toast.show({
          type: 'error',
          text1: 'Connection Error',
          text2: 'Please check your internet connection',
        });
      } else {
        setError(errorMessage);
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
          disabled={loading || (rateLimitedUntil !== null && countdown > 0)}
          style={styles.button}
        >
          {countdown > 0 ? `Try again in ${formatCountdown(countdown)}` : 'Send OTP'}
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

