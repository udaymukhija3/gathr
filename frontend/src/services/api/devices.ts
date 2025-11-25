import { apiRequest, MOCK_MODE } from './client';

type DeviceResponse = { id: number; deviceType: string; deviceName: string; isActive: boolean };

export const devicesApi = {
  register: async (deviceToken: string, deviceType: 'IOS' | 'ANDROID' | 'WEB', deviceName?: string): Promise<DeviceResponse> => {
    if (MOCK_MODE) {
      console.log('Mock: Registering device', { deviceToken, deviceType, deviceName });
      return {
        id: Math.floor(Math.random() * 1000),
        deviceType,
        deviceName: deviceName || 'Mock Device',
        isActive: true,
      };
    }
    return apiRequest('/devices/register', {
      method: 'POST',
      body: JSON.stringify({ deviceToken, deviceType, deviceName }),
    });
  },

  unregister: async (deviceId: number): Promise<void> => {
    if (MOCK_MODE) {
      console.log('Mock: Unregistering device', deviceId);
      return;
    }
    await apiRequest(`/devices/${deviceId}`, {
      method: 'DELETE',
    });
  },

  getMyDevices: async (): Promise<DeviceResponse[]> => {
    if (MOCK_MODE) {
      return [];
    }
    return apiRequest('/devices');
  },
};

