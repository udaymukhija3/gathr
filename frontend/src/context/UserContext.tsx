import React, { createContext, useContext, useState, useEffect, useRef, ReactNode } from 'react';
import * as Notifications from 'expo-notifications';
import { User, AuthResponse } from '../types';
import { getToken, setToken, clearToken, usersApi } from '../services/api';
import {
  addNotificationReceivedListener,
  addNotificationResponseReceivedListener,
} from '../services/pushNotifications';
import {
  registerDeviceWithBackend,
  unregisterDeviceFromBackend,
} from '../services/pushRegistration';
import { authEvents } from '../utils/AuthEvents';

interface UserContextType {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  pushToken: string | null;
  login: (authResponse: AuthResponse) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const UserContext = createContext<UserContextType | undefined>(undefined);

export const useUser = () => {
  const context = useContext(UserContext);
  if (!context) {
    throw new Error('useUser must be used within UserProvider');
  }
  return context;
};

interface UserProviderProps {
  children: ReactNode;
}

export const UserProvider: React.FC<UserProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setTokenState] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pushToken, setPushToken] = useState<string | null>(null);
  const [deviceId, setDeviceId] = useState<number | null>(null);

  const notificationListener = useRef<Notifications.Subscription>();
  const responseListener = useRef<Notifications.Subscription>();



  useEffect(() => {
    checkAuth();

    // Listen for unauthorized events (401)
    const handleUnauthorized = () => {
      logout();
    };
    authEvents.on('unauthorized', handleUnauthorized);

    // Set up notification listeners
    notificationListener.current = addNotificationReceivedListener((notification) => {
      console.log('Notification received:', notification);
    });

    responseListener.current = addNotificationResponseReceivedListener((response) => {
      console.log('Notification response:', response);
      // Handle notification tap - navigation would be handled by the component using this
    });

    return () => {
      authEvents.off('unauthorized', handleUnauthorized);
      if (notificationListener.current) {
        Notifications.removeNotificationSubscription(notificationListener.current);
      }
      if (responseListener.current) {
        Notifications.removeNotificationSubscription(responseListener.current);
      }
    };
  }, []);

  // Register device for push notifications when user logs in
  useEffect(() => {
    if (token && !pushToken) {
      registerDeviceForPush();
    }
  }, [token]);

  const checkAuth = async () => {
    try {
      const storedToken = await getToken();
      if (storedToken) {
        setTokenState(storedToken);
        // Fetch user profile to validate token and get user data
        try {
          const userProfile = await usersApi.getMe();
          setUser({
            id: userProfile.id,
            name: userProfile.name,
            phone: userProfile.phone,
            verified: true,
            createdAt: new Date().toISOString(),
            bio: userProfile.bio,
            interests: userProfile.interests,
            homeHubId: userProfile.homeHubId,
            onboardingCompleted: Boolean(userProfile.onboardingCompleted),
          });
        } catch (error) {
          console.error('Error fetching user profile:', error);
          // Token might be invalid, clear it
          await clearToken();
          setTokenState(null);
        }
      }
    } catch (error) {
      console.error('Error checking auth:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const registerDeviceForPush = async () => {
    try {
      const result = await registerDeviceWithBackend();
      if (!result) {
        return;
      }
      setPushToken(result.expoPushToken);
      setDeviceId(result.deviceId);
      console.log('Device registered for push notifications:', result);
    } catch (error) {
      console.error('Error registering device for push:', error);
    }
  };

  const login = async (authResponse: AuthResponse) => {
    await setToken(authResponse.token);
    setTokenState(authResponse.token);
    setUser({
      ...authResponse.user,
      onboardingCompleted: Boolean(authResponse.user.onboardingCompleted),
    });
  };

  const logout = async () => {
    // Unregister device before logout
    if (deviceId) {
      try {
        await unregisterDeviceFromBackend(deviceId);
      } catch (error) {
        console.error('Error unregistering device:', error);
      }
    }

    await clearToken();
    setTokenState(null);
    setUser(null);
    setPushToken(null);
    setDeviceId(null);
  };

  const refreshUser = async () => {
    try {
      const userProfile = await usersApi.getMe();
      setUser({
        id: userProfile.id,
        name: userProfile.name,
        phone: userProfile.phone,
        verified: true,
        createdAt: new Date().toISOString(),
        bio: userProfile.bio,
        interests: userProfile.interests,
        homeHubId: userProfile.homeHubId,
        onboardingCompleted: Boolean(userProfile.onboardingCompleted),
      });
    } catch (error) {
      console.error('Error refreshing user:', error);
      // Optional: Set a global error state here if UI needs to show it
      // setError(error); 
    }
  };

  return (
    <UserContext.Provider
      value={{
        user,
        token,
        isLoading,
        pushToken,
        login,
        logout,
        refreshUser,
      }}
    >
      {children}
    </UserContext.Provider>
  );
};

