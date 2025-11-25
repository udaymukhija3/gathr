import { useState, useEffect, useRef } from 'react';
import * as Notifications from 'expo-notifications';
import { useUser } from '../context/UserContext';
import {
  addNotificationReceivedListener,
  addNotificationResponseReceivedListener,
} from '../services/pushNotifications';
import {
  registerDeviceWithBackend,
  unregisterDeviceFromBackend,
} from '../services/pushRegistration';

interface UsePushNotificationsReturn {
  expoPushToken: string | null;
  notification: Notifications.Notification | null;
  deviceId: number | null;
  isRegistered: boolean;
  registerDevice: () => Promise<void>;
  unregisterDevice: () => Promise<void>;
}

export function usePushNotifications(
  onNotificationReceived?: (notification: Notifications.Notification) => void,
  onNotificationResponse?: (response: Notifications.NotificationResponse) => void
): UsePushNotificationsReturn {
  const { user, token } = useUser();
  const [expoPushToken, setExpoPushToken] = useState<string | null>(null);
  const [notification, setNotification] = useState<Notifications.Notification | null>(null);
  const [deviceId, setDeviceId] = useState<number | null>(null);
  const [isRegistered, setIsRegistered] = useState(false);

  const notificationListener = useRef<Notifications.Subscription>();
  const responseListener = useRef<Notifications.Subscription>();

  useEffect(() => {
    // Set up notification listeners
    notificationListener.current = addNotificationReceivedListener((notification) => {
      setNotification(notification);
      onNotificationReceived?.(notification);
    });

    responseListener.current = addNotificationResponseReceivedListener((response) => {
      onNotificationResponse?.(response);
    });

    return () => {
      if (notificationListener.current) {
        Notifications.removeNotificationSubscription(notificationListener.current);
      }
      if (responseListener.current) {
        Notifications.removeNotificationSubscription(responseListener.current);
      }
    };
  }, [onNotificationReceived, onNotificationResponse]);

  // Auto-register when user logs in
  useEffect(() => {
    if (user && token && !isRegistered) {
      registerDevice();
    }
  }, [user, token]);

  const registerDevice = async () => {
    try {
      const result = await registerDeviceWithBackend();
      if (!result) {
        console.log('Could not get push token');
        return;
      }
      setExpoPushToken(result.expoPushToken);
      setDeviceId(result.deviceId);
      setIsRegistered(true);
      console.log('Device registered:', result);
    } catch (error) {
      console.error('Error registering device:', error);
    }
  };

  const unregisterDevice = async () => {
    try {
      if (deviceId) {
        await unregisterDeviceFromBackend(deviceId);
        setDeviceId(null);
        setIsRegistered(false);
        console.log('Device unregistered');
      }
    } catch (error) {
      console.error('Error unregistering device:', error);
    }
  };

  return {
    expoPushToken,
    notification,
    deviceId,
    isRegistered,
    registerDevice,
    unregisterDevice,
  };
}
