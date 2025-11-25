import { AuthResponse, User } from '../../types';
import { apiRequest, MOCK_MODE, setToken } from './client';

export const authApi = {
  startOtp: async (phone: string): Promise<void> => {
    if (MOCK_MODE) {
      console.log('Mock: Sending OTP to', phone);
      return;
    }
    await apiRequest('/auth/otp/start', {
      method: 'POST',
      body: JSON.stringify({ phone }),
    });
  },

  verifyOtp: async (phone: string, otp: string): Promise<AuthResponse> => {
    if (MOCK_MODE) {
      const mockUser: User = {
        id: Math.floor(Math.random() * 1000),
        name: phone,
        phone,
        verified: true,
        createdAt: new Date().toISOString(),
      };
      const mockToken = 'mock_jwt_token_' + Date.now();
      await setToken(mockToken);
      return { token: mockToken, user: mockUser };
    }
    const response = await apiRequest<AuthResponse>('/auth/otp/verify', {
      method: 'POST',
      body: JSON.stringify({ phone, otp }),
    });
    await setToken(response.token);
    return response;
  },
};

