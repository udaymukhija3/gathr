import '@testing-library/jest-native/extend-expect';

// Simplify react-native-paper Portal/Modal behavior in tests
jest.mock('react-native-paper', () => {
  const actual = jest.requireActual('react-native-paper');
  const React = require('react');

  const renderChildren = (children) => React.createElement(React.Fragment, null, children);

  const Portal = ({ children }) => renderChildren(children);
  Portal.Host = ({ children }) => renderChildren(children);

  const Modal = ({ visible = true, children }) =>
    visible ? renderChildren(children) : null;

  return {
    ...actual,
    Portal,
    Modal,
  };
});

// Ensure React Native Modal renders content (and testIDs) in tests
jest.mock('react-native/Libraries/Modal/Modal', () => {
  const React = require('react');
  const { View } = require('react-native');
  return ({ children, visible = true, ...props }) =>
    visible ? React.createElement(View, props, children) : null;
});

// Prevent vector-icons/font loading issues in tests
jest.mock('react-native-vector-icons/MaterialCommunityIcons', () => {
  const React = require('react');
  return (props: any) => React.createElement('Icon', props, props.children);
});

jest.mock('@expo/vector-icons', () => {
  const React = require('react');
  const Icon = (props: any) => React.createElement('Icon', props, props.children);
  return new Proxy(
    {},
    {
      get: () => Icon,
    }
  );
});

// Mock expo-secure-store
jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(() => Promise.resolve(null)),
  setItemAsync: jest.fn(() => Promise.resolve()),
  deleteItemAsync: jest.fn(() => Promise.resolve()),
}));

// Mock react-native-toast-message
jest.mock('react-native-toast-message', () => ({
  show: jest.fn(),
  hide: jest.fn(),
}));

// Mock expo-clipboard
jest.mock('expo-clipboard', () => ({
  setStringAsync: jest.fn(() => Promise.resolve()),
  getStringAsync: jest.fn(() => Promise.resolve('')),
}));

// Mock expo-sharing
jest.mock('expo-sharing', () => ({
  isAvailableAsync: jest.fn(() => Promise.resolve(true)),
  shareAsync: jest.fn(() => Promise.resolve()),
}));

// Mock expo-contacts
jest.mock('expo-contacts', () => ({
  requestPermissionsAsync: jest.fn(() => Promise.resolve({ status: 'granted' })),
  getContactsAsync: jest.fn(() => Promise.resolve({ data: [] })),
}));

// Mock @react-navigation/native
jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: () => ({
    navigate: jest.fn(),
    goBack: jest.fn(),
    reset: jest.fn(),
  }),
  useRoute: () => ({
    params: {},
  }),
}));

// Suppress console warnings in tests
global.console = {
  ...console,
  warn: jest.fn(),
  error: jest.fn(),
};
