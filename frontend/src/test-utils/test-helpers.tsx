import React, { ReactElement } from 'react';
import { render, RenderOptions } from '@testing-library/react-native';
import { Provider as PaperProvider, Portal } from 'react-native-paper';
import { SafeAreaProvider, initialWindowMetrics } from 'react-native-safe-area-context';
import { NavigationContainer } from '@react-navigation/native';
import { UserProvider } from '../context/UserContext';
import { theme } from '../theme';

interface AllTheProvidersProps {
  children: React.ReactNode;
}

const defaultMetrics =
  initialWindowMetrics ?? {
    frame: { x: 0, y: 0, width: 375, height: 667 },
    insets: { top: 0, left: 0, right: 0, bottom: 0 },
  };

const AllTheProviders = ({ children }: AllTheProvidersProps) => {
  return (
    <SafeAreaProvider initialMetrics={defaultMetrics}>
      <PaperProvider theme={theme}>
        <Portal.Host>
          <UserProvider>
            <NavigationContainer>
              {children}
            </NavigationContainer>
          </UserProvider>
        </Portal.Host>
      </PaperProvider>
    </SafeAreaProvider>
  );
};

const customRender = (
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'>
) => render(ui, { wrapper: AllTheProviders, ...options });

// Re-export everything
export * from '@testing-library/react-native';
export { customRender as render };

// Mock user for authenticated tests
export const mockUser = {
  id: 1,
  name: 'Test User',
  phone: '1234567890',
  verified: true,
  createdAt: new Date().toISOString(),
  onboardingCompleted: true,
};

// Mock activity
export const mockActivity = {
  id: 1,
  title: 'Coffee at Galleria',
  hubId: 1,
  hubName: 'Galleria',
  category: 'FOOD' as const,
  startTime: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
  endTime: new Date(Date.now() + 4 * 60 * 60 * 1000).toISOString(),
  createdBy: 1,
  createdByName: 'Test User',
  peopleCount: 5,
  mutualsCount: 2,
  isInviteOnly: false,
  revealIdentities: false,
  maxMembers: 4,
};

// Mock template
export const mockTemplate = {
  id: 1,
  name: 'Coffee Meetup',
  title: 'Coffee at Galleria',
  category: 'FOOD' as const,
  durationHours: 2,
  description: 'Casual coffee meetup',
  isSystemTemplate: true,
  isInviteOnly: false,
  maxMembers: 4,
};
