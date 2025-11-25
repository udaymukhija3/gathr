import React from 'react';
import { Text } from 'react-native';
import { render, waitFor } from '@testing-library/react-native';
import { AppNavigator } from '../AppNavigator';
import { useUser } from '../../context/UserContext';

jest.mock('../../context/UserContext');

const mockScreen = (label: string) => () => <Text>{label}</Text>;

jest.mock('../../screens/PhoneEntryScreen', () => ({
  PhoneEntryScreen: mockScreen('Phone Entry Screen'),
}));

jest.mock('../../screens/OtpVerifyScreen', () => ({
  OtpVerifyScreen: mockScreen('OTP Verify Screen'),
}));

jest.mock('../../screens/OnboardingScreen', () => ({
  OnboardingScreen: mockScreen('Onboarding Screen'),
}));

jest.mock('../../screens/FeedScreen', () => ({
  FeedScreen: mockScreen('Feed Screen'),
}));

jest.mock('../../screens/MapViewScreen', () => ({
  MapViewScreen: mockScreen('Map Screen'),
}));

jest.mock('../../screens/TemplateSelectionScreen', () => ({
  TemplateSelectionScreen: mockScreen('Template Selection Screen'),
}));

jest.mock('../../screens/ChatsScreen', () => ({
  ChatsScreen: mockScreen('Chats Screen'),
}));

jest.mock('../../screens/ProfileScreen', () => ({
  ProfileScreen: mockScreen('Profile Screen'),
}));

jest.mock('../../screens/ActivityDetailScreen', () => ({
  ActivityDetailScreen: mockScreen('Activity Detail Screen'),
}));

jest.mock('../../screens/ChatScreen', () => ({
  ChatScreen: mockScreen('Chat Screen'),
}));

jest.mock('../../screens/InviteScreen', () => ({
  InviteScreen: mockScreen('Invite Screen'),
}));

jest.mock('../../screens/CreateActivityScreen', () => ({
  CreateActivityScreen: mockScreen('Create Activity Screen'),
}));

jest.mock('../../screens/ContactsUploadScreen', () => ({
  ContactsUploadScreen: mockScreen('Contacts Upload Screen'),
}));

jest.mock('../../screens/SettingsScreen', () => ({
  SettingsScreen: mockScreen('Settings Screen'),
}));

jest.mock('../../screens/MyActivitiesScreen', () => ({
  MyActivitiesScreen: mockScreen('My Activities Screen'),
}));

jest.mock('../../screens/NotificationsScreen', () => ({
  NotificationsScreen: mockScreen('Notifications Screen'),
}));

const baseContext = {
  user: null,
  token: null,
  pushToken: null,
  isLoading: false,
  login: jest.fn(),
  logout: jest.fn(),
  refreshUser: jest.fn(),
};

const mockUseUser = useUser as jest.Mock;

describe('AppNavigator', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders auth flow when no user is present', () => {
    mockUseUser.mockReturnValue(baseContext);

    const { getByText } = render(<AppNavigator />);

    expect(getByText('Phone Entry Screen')).toBeTruthy();
  });

  it('forces onboarding when user has not completed the flow', () => {
    mockUseUser.mockReturnValue({
      ...baseContext,
      user: {
        id: 1,
        name: 'Test User',
        phone: '1234567890',
        verified: true,
        createdAt: new Date().toISOString(),
        onboardingCompleted: false,
      },
    });

    const { getByText } = render(<AppNavigator />);

    expect(getByText('Onboarding Screen')).toBeTruthy();
  });

  it('renders the bottom tabs when onboarding is completed', async () => {
    mockUseUser.mockReturnValue({
      ...baseContext,
      user: {
        id: 1,
        name: 'Test User',
        phone: '1234567890',
        verified: true,
        createdAt: new Date().toISOString(),
        onboardingCompleted: true,
      },
    });

    const { getByTestId } = render(<AppNavigator />);

    await waitFor(() => {
      expect(getByTestId('tab-home')).toBeTruthy();
      expect(getByTestId('tab-map')).toBeTruthy();
      expect(getByTestId('tab-create')).toBeTruthy();
      expect(getByTestId('tab-chat')).toBeTruthy();
      expect(getByTestId('tab-profile')).toBeTruthy();
    });
  });
});


