import { devicesApi } from './api';
import {
  addNotificationReceivedListener,
  addNotificationResponseReceivedListener,
  getDeviceName,
  getDeviceType,
  registerForPushNotificationsAsync,
} from './pushNotifications';

export type PushRegistrationResult = {
  expoPushToken: string;
  deviceId: number;
};

export const registerDeviceWithBackend = async (): Promise<PushRegistrationResult | null> => {
  const pushToken = await registerForPushNotificationsAsync();
  if (!pushToken) {
    return null;
  }

  const deviceType = getDeviceType();
  const deviceName = getDeviceName();
  const response = await devicesApi.register(pushToken, deviceType, deviceName);
  return {
    expoPushToken: pushToken,
    deviceId: response.id,
  };
};

export const unregisterDeviceFromBackend = async (deviceId?: number): Promise<void> => {
  if (!deviceId) {
    return;
  }
  await devicesApi.unregister(deviceId);
};

export const notificationListeners = {
  addReceivedListener: addNotificationReceivedListener,
  addResponseListener: addNotificationResponseReceivedListener,
};

