import React from 'react';
import { Provider as PaperProvider, Portal } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import Toast from 'react-native-toast-message';
import { UserProvider } from './src/context/UserContext';
import { ErrorBoundary } from './src/components/ErrorBoundary';
import { theme } from './src/theme';
import { AppNavigator } from './src/navigation/AppNavigator';

export default function App() {
  return (
    <ErrorBoundary>
      <UserProvider>
        <SafeAreaProvider>
          <PaperProvider theme={theme}>
            <Portal.Host>
              <StatusBar style="auto" />
              <AppNavigator />
            </Portal.Host>
            <Toast />
          </PaperProvider>
        </SafeAreaProvider>
      </UserProvider>
    </ErrorBoundary>
  );
}

