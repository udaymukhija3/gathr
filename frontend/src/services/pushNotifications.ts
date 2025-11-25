import * as Notifications from 'expo-notifications';
import * as Device from 'expo-device';
import { Platform } from 'react-native';
import Constants from 'expo-constants';

// Types
export interface PushNotificationState {
  expoPushToken: string | null;
  notification: Notifications.Notification | null;
}

export type DeviceType = 'IOS' | 'ANDROID' | 'WEB';

// Configure notification behavior
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

/**
 * Register for push notifications and get the Expo push token.
 * Returns null if running on simulator/emulator or if permission denied.
 */
export async function registerForPushNotificationsAsync(): Promise<string | null> {
  let token: string | null = null;

  // Must be a physical device
  if (!Device.isDevice) {
    console.log('Push notifications require a physical device');
    return null;
  }

  // Check/request permissions
  const { status: existingStatus } = await Notifications.getPermissionsAsync();
  let finalStatus = existingStatus;

  if (existingStatus !== 'granted') {
    const { status } = await Notifications.requestPermissionsAsync();
    finalStatus = status;
  }

  if (finalStatus !== 'granted') {
    console.log('Permission for push notifications denied');
    return null;
  }

  // Get push token
  try {
    // For Expo Go, use Expo push token
    // For standalone apps, you'd use FCM/APNs directly
    const projectId = Constants.expoConfig?.extra?.eas?.projectId
      ?? Constants.easConfig?.projectId;

    if (projectId) {
      const tokenResponse = await Notifications.getExpoPushTokenAsync({
        projectId,
      });
      token = tokenResponse.data;
    } else {
      // Fallback for development
      const tokenResponse = await Notifications.getExpoPushTokenAsync();
      token = tokenResponse.data;
    }

    console.log('Push token:', token);
  } catch (error) {
    console.error('Error getting push token:', error);
  }

  // Android-specific channel configuration
  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name: 'Default',
      importance: Notifications.AndroidImportance.MAX,
      vibrationPattern: [0, 250, 250, 250],
      lightColor: '#6200EE',
    });

    await Notifications.setNotificationChannelAsync('activities', {
      name: 'Activities',
      description: 'Notifications about activities you joined',
      importance: Notifications.AndroidImportance.HIGH,
      vibrationPattern: [0, 250, 250, 250],
      lightColor: '#6200EE',
    });

    await Notifications.setNotificationChannelAsync('chat', {
      name: 'Chat Messages',
      description: 'New messages in activity chats',
      importance: Notifications.AndroidImportance.HIGH,
      vibrationPattern: [0, 100, 100, 100],
      lightColor: '#6200EE',
    });

    await Notifications.setNotificationChannelAsync('reminders', {
      name: 'Reminders',
      description: 'Activity reminders',
      importance: Notifications.AndroidImportance.HIGH,
      vibrationPattern: [0, 500],
      lightColor: '#FFC107',
    });
  }

  return token;
}

/**
 * Get the device type for the backend
 */
export function getDeviceType(): DeviceType {
  if (Platform.OS === 'ios') return 'IOS';
  if (Platform.OS === 'android') return 'ANDROID';
  return 'WEB';
}

/**
 * Get a friendly device name
 */
export function getDeviceName(): string {
  const brand = Device.brand || 'Unknown';
  const modelName = Device.modelName || 'Device';
  return `${brand} ${modelName}`;
}

/**
 * Add a listener for incoming notifications (when app is foregrounded)
 */
export function addNotificationReceivedListener(
  callback: (notification: Notifications.Notification) => void
): Notifications.Subscription {
  return Notifications.addNotificationReceivedListener(callback);
}

/**
 * Add a listener for notification responses (when user taps notification)
 */
export function addNotificationResponseReceivedListener(
  callback: (response: Notifications.NotificationResponse) => void
): Notifications.Subscription {
  return Notifications.addNotificationResponseReceivedListener(callback);
}

/**
 * Schedule a local notification (useful for testing)
 */
export async function scheduleLocalNotification(
  title: string,
  body: string,
  data?: Record<string, any>,
  seconds: number = 1
): Promise<string> {
  return await Notifications.scheduleNotificationAsync({
    content: {
      title,
      body,
      data,
      sound: true,
    },
    trigger: { seconds },
  });
}

/**
 * Cancel all scheduled notifications
 */
export async function cancelAllNotifications(): Promise<void> {
  await Notifications.cancelAllScheduledNotificationsAsync();
}

/**
 * Get the badge count
 */
export async function getBadgeCount(): Promise<number> {
  return await Notifications.getBadgeCountAsync();
}

/**
 * Set the badge count
 */
export async function setBadgeCount(count: number): Promise<void> {
  await Notifications.setBadgeCountAsync(count);
}

/**
 * Dismiss all notifications from notification center
 */
export async function dismissAllNotifications(): Promise<void> {
  await Notifications.dismissAllNotificationsAsync();
}
